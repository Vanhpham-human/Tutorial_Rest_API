package com.fptaptech.catalog.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public enum ApiErrorCode {
    VALIDATION_FAILED(400, "Validation failed", "Invalid request content.", "/api/orders",
            List.of(new ApiFieldError("items", "must not be empty"))),
    MALFORMED_JSON(400, "Malformed JSON", "Body không phải JSON hợp lệ", "/api/orders", List.of()),
    IDEMPOTENCY_KEY_REQUIRED(400, "Idempotency key required", "Header Idempotency-Key là bắt buộc", "/api/orders", List.of()),
    REASON_REQUIRED(400, "Reason required", "Hủy đơn CONFIRMED bắt buộc có reason", "/api/orders/ORD-2026-000001/transitions", List.of()),
    INVALID_SORT(400, "Invalid sort", "sort chỉ nhận createdAt hoặc totalAmount kèm asc/desc", "/api/orders", List.of()),
    INVALID_DATE_RANGE(400, "Invalid date range", "from phải nhỏ hơn hoặc bằng to", "/api/orders", List.of()),
    UNSUPPORTED_API_VERSION(400, "Unsupported API version", "X-API-Version chỉ nhận 1 hoặc 2", "/api/orders/ORD-2026-000001", List.of()),
    DUPLICATE_PRODUCT(400, "Duplicate product", "Mỗi productId chỉ xuất hiện một lần trong đơn", "/api/orders", List.of()),
    UNAUTHORIZED(401, "Unauthorized", "Thiếu hoặc sai token", "/api/orders", List.of()),
    PARTNER_KEY_INVALID(401, "Invalid partner key", "Header X-Partner-Key không hợp lệ", "/api/webhooks/shipping", List.of()),
    FORBIDDEN(403, "Forbidden", "Không có quyền thực hiện thao tác này", "/api/orders/ORD-2026-000001/transitions", List.of()),
    PRODUCT_NOT_FOUND(404, "Product not found", "Không tìm thấy sản phẩm id=99", "/api/orders", List.of()),
    ORDER_NOT_FOUND(404, "Order not found", "Không tìm thấy đơn ORD-2026-000999", "/api/orders/ORD-2026-000999", List.of()),
    INVOICE_NOT_FOUND(404, "Invoice not found", "Đơn chưa có hóa đơn", "/api/orders/ORD-2026-000001/invoice", List.of()),
    IDEMPOTENCY_CONFLICT(409, "Idempotency conflict", "Idempotency-Key đã dùng với nội dung khác", "/api/orders", List.of()),
    ORDER_INVALID_TRANSITION(409, "Invalid transition", "Không thể CANCEL khi đơn đang ở trạng thái SHIPPING", "/api/orders/ORD-2026-000001/transitions", List.of()),
    RETURN_WINDOW_EXPIRED(409, "Return window expired", "Chỉ được yêu cầu trả hàng trong 7 ngày kể từ khi giao", "/api/orders/ORD-2026-000001/transitions", List.of()),
    PRECONDITION_FAILED(412, "Precondition failed", "If-Match không khớp version hiện tại", "/api/orders/ORD-2026-000001/transitions", List.of()),
    PRECONDITION_REQUIRED(428, "Precondition required", "Header If-Match là bắt buộc", "/api/orders/ORD-2026-000001/transitions", List.of()),
    INVOICE_TOO_LARGE(413, "Invoice too large", "Hóa đơn vượt quá 5MB", "/api/orders/ORD-2026-000001/invoice", List.of()),
    INVOICE_NOT_PDF(415, "Invoice is not a PDF", "Chỉ nhận file PDF", "/api/orders/ORD-2026-000001/invoice", List.of());

    private final int status;
    private final String title;
    private final Map<String, Object> example;

    ApiErrorCode(int status, String title, String detail, String instance, List<ApiFieldError> errors) {
        this.status = status;
        this.title = title;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", ProblemResponses.ERROR_BASE + ProblemResponses.slug(name()));
        body.put("title", title);
        body.put("status", status);
        body.put("detail", detail);
        body.put("instance", instance);
        body.put("code", name());
        body.put("timestamp", "2026-10-16T03:12:45.120Z");
        if (errors != null && !errors.isEmpty()) {
            body.put("errors", errors.stream()
                    .map(error -> Map.of("field", error.field(), "message", error.message()))
                    .toList());
        }
        this.example = Map.copyOf(body);
    }

    public int status() {
        return status;
    }

    public String title() {
        return title;
    }

    public Map<String, Object> example() {
        return example;
    }
}
