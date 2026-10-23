package com.fptaptech.catalog.order;

import com.fptaptech.catalog.common.CatalogException;
import com.fptaptech.catalog.common.CurrentUser;
import com.fptaptech.catalog.customer.Customer;
import com.fptaptech.catalog.customer.CustomerRepository;
import com.fptaptech.catalog.payment.BankTransferPayment;
import com.fptaptech.catalog.payment.CardPayment;
import com.fptaptech.catalog.payment.CodPayment;
import com.fptaptech.catalog.payment.Payment;
import com.fptaptech.catalog.product.Product;
import com.fptaptech.catalog.product.ProductNotFoundException;
import com.fptaptech.catalog.product.ProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class OrderService {

    public static final String CURRENCY = "VND";
    private static final Set<String> SORTS = Set.of(
            "createdAt,asc", "createdAt,desc", "totalAmount,asc", "totalAmount,desc");
    private static final long MAX_INVOICE_BYTES = 5L * 1024 * 1024;

    private final OrderRepository orders;
    private final OrderEventRepository events;
    private final OrderSequenceRepository sequences;
    private final InvoiceRepository invoices;
    private final IdempotencyRepository idempotency;
    private final CustomerRepository customers;
    private final ProductRepository products;
    private final OrderMapper mapper;
    private final OrderStateMachine stateMachine;
    private final Clock clock;

    public OrderService(OrderRepository orders, OrderEventRepository events, OrderSequenceRepository sequences,
                        InvoiceRepository invoices, IdempotencyRepository idempotency, CustomerRepository customers,
                        ProductRepository products, OrderMapper mapper, Clock clock) {
        this.orders = orders;
        this.events = events;
        this.sequences = sequences;
        this.invoices = invoices;
        this.idempotency = idempotency;
        this.customers = customers;
        this.products = products;
        this.mapper = mapper;
        this.stateMachine = new OrderStateMachine();
        this.clock = clock;
    }

    public OrderPage list(OrderSearchQuery query, CurrentUser user) {
        int page = query.page() == null ? 0 : query.page();
        int size = query.size() == null ? 20 : query.size();
        String sortKey = query.sort() == null || query.sort().isBlank() ? "createdAt,desc" : query.sort();
        if (!SORTS.contains(sortKey)) {
            throw badRequest("INVALID_SORT", "Invalid sort", "sort chỉ nhận createdAt hoặc totalAmount kèm asc/desc");
        }
        if (query.from() != null && query.to() != null && query.from().isAfter(query.to())) {
            throw badRequest("INVALID_DATE_RANGE", "Invalid date range", "from phải nhỏ hơn hoặc bằng to");
        }
        Long customerId = user.admin() ? null : user.customerId();
        Instant from = query.from() == null ? null : query.from().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = query.to() == null ? null : query.to().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Specification<Order> spec = (root, cq, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (customerId != null) {
                predicates.add(cb.equal(root.get("customer").get("id"), customerId));
            }
            if (query.status() != null) {
                predicates.add(cb.equal(root.get("status"), query.status()));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), to));
            }
            if (query.minTotal() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalAmount"), query.minTotal()));
            }
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        Page<Order> result = orders.findAll(spec, PageRequest.of(page, size, sortOf(sortKey)));
        List<OrderSummary> content = result.getContent().stream().map(this::summary).toList();
        return new OrderPage(content, result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages(), result.isFirst(), result.isLast());
    }

    public OrderDetailV1 detailV1(String code, CurrentUser user) {
        return toV1(visible(code, user));
    }

    public OrderDetailV2 detailV2(String code, CurrentUser user) {
        return toV2(visible(code, user));
    }

    @Transactional
    public CreationResult create(CreateOrderRequest request, CurrentUser user, String idempotencyKey, String requestHash) {
        if (user.admin() || user.customerId() == null) {
            throw forbidden("Chỉ CUSTOMER được tạo đơn");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw badRequest("IDEMPOTENCY_KEY_REQUIRED", "Idempotency key required", "Header Idempotency-Key là bắt buộc");
        }
        String key = idempotencyKey.trim();
        if (key.length() > 200) {
            throw badRequest("IDEMPOTENCY_KEY_REQUIRED", "Idempotency key required", "Idempotency-Key tối đa 200 ký tự");
        }
        var existing = idempotency.findByCustomerIdAndIdempotencyKey(user.customerId(), key);
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (record.getCreatedAt().isBefore(Instant.now(clock).minus(24, ChronoUnit.HOURS))) {
                idempotency.delete(record);
                idempotency.flush();
            } else if (!record.getRequestHash().equals(requestHash)) {
                throw conflict("IDEMPOTENCY_CONFLICT", "Idempotency conflict", "Idempotency-Key đã dùng với nội dung khác");
            } else {
                return new CreationResult(readCreated(record.getResponseBody()), record.getLocation(), true);
            }
        }
        CreationResult created = insertOrder(request, user);
        IdempotencyRecord record = new IdempotencyRecord();
        record.setCustomerId(user.customerId());
        record.setIdempotencyKey(key);
        record.setRequestHash(requestHash);
        record.setResponseBody(writeCreated(created.body()));
        record.setLocation(created.location());
        record.setCreatedAt(Instant.now(clock));
        idempotency.save(record);
        return created;
    }

    @Transactional
    public TransitionResponse transition(String code, TransitionRequest request, String ifMatch, CurrentUser user) {
        Order order = visible(code, user);
        long expected = com.fptaptech.catalog.common.Etags.parse(ifMatch);
        if (order.getVersion() == null || !order.getVersion().equals(expected)) {
            throw conflictStatus(HttpStatus.PRECONDITION_FAILED, "PRECONDITION_FAILED", "Precondition failed",
                    "If-Match không khớp version hiện tại");
        }
        ActorRole role = user.admin() ? ActorRole.ADMIN : ActorRole.CUSTOMER;
        apply(order, request.action(), request.reason(), role, user.email());
        return new TransitionResponse(order.getCode(), order.getStatus(), order.getVersion());
    }

    @Transactional
    public TransitionResponse partner(String code, OrderAction action, String reason) {
        Order order = orders.findByCode(code).orElseThrow(() -> notFound(code));
        apply(order, action, reason, ActorRole.PARTNER, "partner");
        return new TransitionResponse(order.getCode(), order.getStatus(), order.getVersion());
    }

    @Transactional
    public InvoiceMetadata storeInvoice(String code, String fileName, String contentType, byte[] content) {
        Order order = orders.findByCode(code).orElseThrow(() -> notFound(code));
        if (content == null || content.length == 0 || content.length > MAX_INVOICE_BYTES) {
            throw content != null && content.length > MAX_INVOICE_BYTES
                    ? conflictStatus(HttpStatus.CONTENT_TOO_LARGE, "INVOICE_TOO_LARGE", "Invoice too large", "Hóa đơn vượt quá 5MB")
                    : conflictStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "INVOICE_NOT_PDF", "Invoice is not a PDF", "Chỉ nhận file PDF");
        }
        if (contentType == null || !(contentType.startsWith("application/pdf") || contentType.startsWith("application/octet-stream"))) {
            throw conflictStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "INVOICE_NOT_PDF", "Invoice is not a PDF", "Chỉ nhận file PDF");
        }
        if (content.length < 4 || content[0] != '%' || content[1] != 'P' || content[2] != 'D' || content[3] != 'F') {
            throw conflictStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "INVOICE_NOT_PDF", "Invoice is not a PDF", "Nội dung không phải PDF");
        }
        Invoice invoice = invoices.findByOrderId(order.getId()).orElseGet(Invoice::new);
        invoice.setOrderId(order.getId());
        invoice.setFileName(sanitize(fileName));
        invoice.setContentType("application/pdf");
        invoice.setSizeBytes(content.length);
        invoice.setContent(content);
        invoice.setUploadedAt(Instant.now(clock));
        invoices.save(invoice);
        return new InvoiceMetadata(order.getCode(), invoice.getFileName(), invoice.getSizeBytes(), invoice.getUploadedAt());
    }

    public Invoice loadInvoice(String code, CurrentUser user) {
        Order order = visible(code, user);
        return invoices.findByOrderId(order.getId()).orElseThrow(() ->
                conflictStatus(HttpStatus.NOT_FOUND, "INVOICE_NOT_FOUND", "Invoice not found", "Đơn chưa có hóa đơn"));
    }

    private CreationResult insertOrder(CreateOrderRequest request, CurrentUser user) {
        Customer customer = customers.findById(user.customerId())
                .orElseThrow(() -> forbidden("Tài khoản không hợp lệ"));
        Set<Long> seen = new HashSet<>();
        for (OrderLineRequest line : request.items()) {
            if (!seen.add(line.productId())) {
                throw badRequest("DUPLICATE_PRODUCT", "Duplicate product", "Mỗi productId chỉ xuất hiện một lần trong đơn");
            }
        }
        Order order = new Order();
        order.setCustomer(customer);
        order.setCode(nextCode());
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress(request.shippingAddress().trim());
        order.setCurrency(CURRENCY);
        order.setCreatedAt(Instant.now(clock));
        applyPayment(order, request.payment());
        BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (OrderLineRequest line : request.items()) {
            Product product = products.findById(line.productId()).orElseThrow(() -> new ProductNotFoundException(line.productId()));
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(line.quantity())).setScale(2, RoundingMode.HALF_UP);
            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setUnitPrice(product.getPrice());
            item.setQuantity(line.quantity());
            item.setLineTotal(lineTotal);
            order.addItem(item);
            total = total.add(lineTotal);
        }
        order.setTotalAmount(total);
        Order saved = orders.saveAndFlush(order);
        OrderCreatedResponse body = new OrderCreatedResponse(
                saved.getCode(), saved.getStatus(), saved.getTotalAmount(), saved.getCurrency(),
                saved.getVersion(), saved.getCreatedAt());
        return new CreationResult(body, "/api/orders/" + saved.getCode(), false);
    }

    private void apply(Order order, OrderAction action, String reason, ActorRole role, String actor) {
            if (!stateMachine.roleSupports(role, action)) {
            if (role == ActorRole.PARTNER) {
                throw conflict("ORDER_INVALID_TRANSITION", "Invalid transition",
                        "Đối tác không được thực hiện " + action);
            }
            throw forbidden("Vai trò " + role + " không được thực hiện " + action);
        }
        if (action == OrderAction.CANCEL && order.getStatus() == OrderStatus.CONFIRMED && (reason == null || reason.isBlank())) {
            throw badRequest("REASON_REQUIRED", "Reason required", "Hủy đơn CONFIRMED bắt buộc có reason");
        }
        OrderStatus next = stateMachine.next(order.getStatus(), action, role).orElseThrow(() ->
                conflict("ORDER_INVALID_TRANSITION", "Invalid transition",
                        "Không thể " + action + " khi đơn đang ở trạng thái " + order.getStatus()));
        if (action == OrderAction.REQUEST_RETURN && windowExpired(order)) {
            throw conflict("RETURN_WINDOW_EXPIRED", "Return window expired",
                    "Chỉ được yêu cầu trả hàng trong 7 ngày kể từ khi giao");
        }
        OrderEvent event = new OrderEvent();
        event.setOrder(order);
        event.setFromStatus(order.getStatus());
        event.setToStatus(next);
        event.setAction(action);
        event.setActor(actor);
        event.setReason(reason == null || reason.isBlank() ? null : reason.trim());
        event.setAt(Instant.now(clock));
        events.save(event);
        order.setStatus(next);
        if (next == OrderStatus.DELIVERED) {
            order.setDeliveredAt(Instant.now(clock));
        }
        try {
            orders.saveAndFlush(order);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw conflictStatus(HttpStatus.PRECONDITION_FAILED, "PRECONDITION_FAILED", "Precondition failed",
                    "Đơn đã được cập nhật, hãy đọc lại ETag");
        }
    }

    private boolean windowExpired(Order order) {
        if (order.getDeliveredAt() == null) {
            return true;
        }
        return Instant.now(clock).isAfter(order.getDeliveredAt().plus(7, ChronoUnit.DAYS));
    }

    private String nextCode() {
        int year = Year.now(clock).getValue();
        OrderSequence sequence = sequences.lock(year).orElseGet(() -> {
            try {
                return sequences.saveAndFlush(new OrderSequence(year, 0));
            } catch (DataIntegrityViolationException ex) {
                return sequences.lock(year).orElseThrow();
            }
        });
        sequence.setLastValue(sequence.getLastValue() + 1);
        return "ORD-%d-%06d".formatted(year, sequence.getLastValue());
    }

    private Order visible(String code, CurrentUser user) {
        Order order = orders.findByCode(code).orElseThrow(() -> notFound(code));
        if (!user.admin() && (user.customerId() == null || !order.getCustomer().getId().equals(user.customerId()))) {
            throw notFound(code);
        }
        return order;
    }

    private void applyPayment(Order order, Payment payment) {
        switch (payment) {
            case CodPayment cod -> {
                if (!"COD".equals(cod.method())) {
                    throw badRequest("VALIDATION_FAILED", "Validation failed", "method COD không khớp kiểu thanh toán");
                }
                order.setPaymentMethod(PaymentMethod.COD);
            }
            case CardPayment card -> {
                if (!"CARD".equals(card.method())) {
                    throw badRequest("VALIDATION_FAILED", "Validation failed", "method CARD không khớp kiểu thanh toán");
                }
                order.setPaymentMethod(PaymentMethod.CARD);
                order.setCardProvider(card.provider());
                order.setCardLast4(card.last4());
            }
            case BankTransferPayment transfer -> {
                if (!"BANK_TRANSFER".equals(transfer.method())) {
                    throw badRequest("VALIDATION_FAILED", "Validation failed", "method BANK_TRANSFER không khớp kiểu thanh toán");
                }
                order.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
                order.setBankCode(transfer.bankCode());
                order.setTransferRef(transfer.transferRef());
            }
        }
    }

    Payment paymentOf(Order order) {
        return switch (order.getPaymentMethod()) {
            case COD -> new CodPayment("COD");
            case CARD -> new CardPayment("CARD", order.getCardProvider(), order.getCardLast4());
            case BANK_TRANSFER -> new BankTransferPayment("BANK_TRANSFER", order.getBankCode(), order.getTransferRef());
        };
    }

    private OrderDetailV1 toV1(Order order) {
        return new OrderDetailV1(order.getCode(), order.getCustomer().getId(), order.getStatus(),
                order.getShippingAddress(), order.getTotalAmount(), order.getCurrency(), order.getVersion(),
                paymentOf(order), mapper.toItems(order.getItems()), order.getCreatedAt(), order.getDeliveredAt());
    }

    private OrderDetailV2 toV2(Order order) {
        return new OrderDetailV2(order.getCode(), order.getCustomer().getId(), order.getStatus(),
                order.getShippingAddress(), new Money(order.getTotalAmount(), order.getCurrency()), order.getVersion(),
                paymentOf(order), mapper.toItems(order.getItems()), order.getCreatedAt(), order.getDeliveredAt());
    }

    private OrderSummary summary(Order order) {
        return new OrderSummary(order.getCode(), order.getStatus(), order.getTotalAmount(), order.getCurrency(), order.getCreatedAt());
    }

    private static Sort sortOf(String sortKey) {
        String[] parts = sortKey.split(",");
        Sort.Direction direction = "asc".equals(parts[1]) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, parts[0]);
    }

    private String writeCreated(OrderCreatedResponse body) {
        return "{\"code\":\"" + body.code() + "\",\"status\":\"" + body.status()
                + "\",\"totalAmount\":" + body.totalAmount().toPlainString()
                + ",\"currency\":\"" + body.currency() + "\",\"version\":" + body.version()
                + ",\"createdAt\":\"" + body.createdAt() + "\"}";
    }

    private OrderCreatedResponse readCreated(String json) {
        String code = extract(json, "code");
        OrderStatus status = OrderStatus.valueOf(extract(json, "status"));
        BigDecimal total = new BigDecimal(extractNumber(json, "totalAmount"));
        String currency = extract(json, "currency");
        long version = Long.parseLong(extractNumber(json, "version"));
        Instant createdAt = Instant.parse(extract(json, "createdAt"));
        return new OrderCreatedResponse(code, status, total, currency, version, createdAt);
    }

    private static String extract(String json, String field) {
        String marker = "\"" + field + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            throw new IllegalStateException("Thiếu trường " + field);
        }
        start += marker.length();
        return json.substring(start, json.indexOf('"', start));
    }

    private static String extractNumber(String json, String field) {
        String marker = "\"" + field + "\":";
        int start = json.indexOf(marker);
        if (start < 0) {
            throw new IllegalStateException("Thiếu trường " + field);
        }
        start += marker.length();
        int end = start;
        while (end < json.length() && ",}".indexOf(json.charAt(end)) < 0) {
            end++;
        }
        return json.substring(start, end);
    }

    private static String sanitize(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "invoice.pdf";
        }
        return fileName.replaceAll("[\\r\\n\\\\\"]", "_");
    }

    private static CatalogException badRequest(String code, String title, String detail) {
        return new CatalogException(HttpStatus.BAD_REQUEST, code, title, detail);
    }

    private static CatalogException forbidden(String detail) {
        return new CatalogException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Forbidden", detail);
    }

    private static CatalogException conflict(String code, String title, String detail) {
        return new CatalogException(HttpStatus.CONFLICT, code, title, detail);
    }

    private static CatalogException conflictStatus(HttpStatus status, String code, String title, String detail) {
        return new CatalogException(status, code, title, detail);
    }

    private static CatalogException notFound(String code) {
        return new CatalogException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found", "Không tìm thấy đơn " + code);
    }
}
