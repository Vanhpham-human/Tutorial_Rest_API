package com.fptaptech.catalog.order;

import com.fptaptech.catalog.common.ApiErrorCode;
import com.fptaptech.catalog.common.ApiErrorResponses;
import com.fptaptech.catalog.common.ApiGroups;
import com.fptaptech.catalog.common.ApiVersions;
import com.fptaptech.catalog.common.CachedBodyFilter;
import com.fptaptech.catalog.common.CatalogException;
import com.fptaptech.catalog.common.CurrentUser;
import com.fptaptech.catalog.common.Etags;
import com.fptaptech.catalog.common.RequestHashes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;

@Validated
@RestController
@Tag(name = "Orders", description = "Đặt và theo dõi đơn hàng")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping(path = "/api/orders", produces = MediaType.APPLICATION_JSON_VALUE)
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @ApiGroups({"public-v1", "public-v2"})
    @Operation(operationId = "createOrder", summary = "Tạo đơn hàng",
            description = "CUSTOMER tạo đơn. Giá do server tính từ catalog. Header Idempotency-Key bắt buộc: cùng key và cùng body trong 24 giờ trả lại response lần đầu; cùng key khác body trả 409 IDEMPOTENCY_CONFLICT.")
    @ApiResponse(responseCode = "201", description = "Đã tạo",
            content = @Content(schema = @Schema(implementation = OrderCreatedResponse.class)))
    @ApiErrorResponses({
            ApiErrorCode.VALIDATION_FAILED, ApiErrorCode.MALFORMED_JSON, ApiErrorCode.IDEMPOTENCY_KEY_REQUIRED,
            ApiErrorCode.DUPLICATE_PRODUCT, ApiErrorCode.PRODUCT_NOT_FOUND, ApiErrorCode.IDEMPOTENCY_CONFLICT,
            ApiErrorCode.UNAUTHORIZED, ApiErrorCode.FORBIDDEN
    })
    @PostMapping
    public ResponseEntity<OrderCreatedResponse> create(
            HttpServletRequest httpRequest,
            @AuthenticationPrincipal Jwt jwt,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateOrderRequest.class),
                            examples = {
                                    @ExampleObject(name = "COD", summary = "Thanh toán khi nhận hàng", value = OrderExamples.COD),
                                    @ExampleObject(name = "CARD", summary = "Thanh toán thẻ", value = OrderExamples.CARD),
                                    @ExampleObject(name = "BANK_TRANSFER", summary = "Chuyển khoản", value = OrderExamples.BANK_TRANSFER)
                            }))
            @Valid @RequestBody CreateOrderRequest request) {
        byte[] raw = (byte[]) httpRequest.getAttribute(CachedBodyFilter.RAW_BODY);
        CreationResult result = orders.create(request, CurrentUser.from(jwt),
                httpRequest.getHeader("Idempotency-Key"), RequestHashes.sha256(raw));
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create(result.location()))
                .eTag(Etags.format(result.body().version()))
                .body(result.body());
    }

    @ApiGroups({"public-v1", "public-v2"})
    @Operation(operationId = "listOrders", summary = "Danh sách đơn",
            description = "Phân trang page/size/sort. sort chỉ createdAt hoặc totalAmount. CUSTOMER chỉ thấy đơn của mình; ADMIN thấy mọi đơn.")
    @ApiResponse(responseCode = "200", description = "Trang đơn hàng",
            content = @Content(schema = @Schema(implementation = OrderPage.class)))
    @ApiErrorResponses({
            ApiErrorCode.VALIDATION_FAILED, ApiErrorCode.INVALID_SORT, ApiErrorCode.INVALID_DATE_RANGE,
            ApiErrorCode.UNAUTHORIZED
    })
    @GetMapping
    public OrderPage list(@ParameterObject @Valid OrderSearchQuery query, @AuthenticationPrincipal Jwt jwt) {
        return orders.list(query, CurrentUser.from(jwt));
    }

    @ApiGroups({"public-v1", "public-v2"})
    @Operation(operationId = "getOrder", summary = "Chi tiết đơn",
            description = "Header X-API-Version: 1 (mặc định, deprecated, ngừng hỗ trợ 2027-04-01) trả totalAmount dạng số. X-API-Version: 2 trả total là object {value, currency}.")
    @ApiResponse(responseCode = "200", description = "Tìm thấy đơn",
            content = @Content(schema = @Schema(oneOf = {OrderDetailV1.class, OrderDetailV2.class})))
    @ApiErrorResponses({
            ApiErrorCode.UNAUTHORIZED, ApiErrorCode.ORDER_NOT_FOUND, ApiErrorCode.UNSUPPORTED_API_VERSION
    })
    @GetMapping("/{code}")
    public ResponseEntity<?> get(
            @Parameter(description = "Mã đơn", example = "ORD-2026-000001") @PathVariable String code,
            @Parameter(name = ApiVersions.HEADER, in = ParameterIn.HEADER,
                    description = "1 (deprecated, ngừng hỗ trợ 2027-04-01) hoặc 2. Mặc định 1.",
                    schema = @Schema(allowableValues = {"1", "2"}, defaultValue = "1"))
            @RequestHeader(name = ApiVersions.HEADER, required = false, defaultValue = ApiVersions.V1) String apiVersion,
            @AuthenticationPrincipal Jwt jwt) {
        CurrentUser user = CurrentUser.from(jwt);
        if (ApiVersions.V1.equals(apiVersion)) {
            OrderDetailV1 body = orders.detailV1(code, user);
            return ResponseEntity.ok().eTag(Etags.format(body.version())).body(body);
        }
        if (ApiVersions.V2.equals(apiVersion)) {
            OrderDetailV2 body = orders.detailV2(code, user);
            return ResponseEntity.ok().eTag(Etags.format(body.version())).body(body);
        }
        throw new CatalogException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_API_VERSION", "Unsupported API version",
                "X-API-Version chỉ nhận 1 hoặc 2");
    }

    @ApiGroups({"public-v1", "public-v2", "admin"})
    @Operation(operationId = "transitionOrder", summary = "Chuyển trạng thái đơn",
            description = "ADMIN được mọi action đúng trạng thái. CUSTOMER chỉ CANCEL khi đơn của mình còn PENDING, và REQUEST_RETURN trong 7 ngày sau DELIVERED. Hủy CONFIRMED chỉ ADMIN và bắt buộc reason. If-Match phải khớp version.")
    @ApiResponse(responseCode = "200", description = "Đã chuyển trạng thái",
            content = @Content(schema = @Schema(implementation = TransitionResponse.class)))
    @ApiErrorResponses({
            ApiErrorCode.VALIDATION_FAILED, ApiErrorCode.MALFORMED_JSON, ApiErrorCode.REASON_REQUIRED,
            ApiErrorCode.UNAUTHORIZED, ApiErrorCode.FORBIDDEN, ApiErrorCode.ORDER_NOT_FOUND,
            ApiErrorCode.ORDER_INVALID_TRANSITION, ApiErrorCode.RETURN_WINDOW_EXPIRED,
            ApiErrorCode.PRECONDITION_FAILED, ApiErrorCode.PRECONDITION_REQUIRED
    })
    @PostMapping("/{code}/transitions")
    public ResponseEntity<TransitionResponse> transition(
            @PathVariable String code,
            @Parameter(name = "If-Match", in = ParameterIn.HEADER, required = true,
                    description = "ETag version hiện tại, ví dụ \"0\"", example = "\"0\"")
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody TransitionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        TransitionResponse body = orders.transition(code, request, ifMatch, CurrentUser.from(jwt));
        return ResponseEntity.ok().eTag(Etags.format(body.version())).body(body);
    }

    @ApiGroups({"admin"})
    @Tag(name = "Invoices")
    @Operation(operationId = "uploadInvoice", tags = {"Invoices"}, summary = "Tải hóa đơn PDF",
            description = "ADMIN upload multipart field file. Chỉ PDF, tối đa 5MB.")
    @ApiResponse(responseCode = "201", description = "Đã lưu hóa đơn",
            content = @Content(schema = @Schema(implementation = InvoiceMetadata.class)))
    @ApiErrorResponses({
            ApiErrorCode.UNAUTHORIZED, ApiErrorCode.FORBIDDEN, ApiErrorCode.ORDER_NOT_FOUND,
            ApiErrorCode.INVOICE_NOT_PDF, ApiErrorCode.INVOICE_TOO_LARGE, ApiErrorCode.VALIDATION_FAILED
    })
    @PostMapping(value = "/{code}/invoice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InvoiceMetadata> uploadInvoice(
            @PathVariable String code,
            @Parameter(description = "File PDF, tối đa 5MB",
                    content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE,
                            schema = @Schema(type = "string", format = "binary")))
            @RequestPart("file") MultipartFile file) throws IOException {
        InvoiceMetadata body = orders.storeInvoice(code, file.getOriginalFilename(), file.getContentType(), file.getBytes());
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/orders/" + code + "/invoice"))
                .body(body);
    }

    @ApiGroups({"public-v1", "public-v2"})
    @Tag(name = "Invoices")
    @Operation(operationId = "downloadInvoice", tags = {"Invoices"}, summary = "Tải hóa đơn PDF")
    @ApiResponse(responseCode = "200", description = "Nội dung PDF",
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE,
                    schema = @Schema(type = "string", format = "binary")))
    @ApiErrorResponses({ApiErrorCode.UNAUTHORIZED, ApiErrorCode.ORDER_NOT_FOUND, ApiErrorCode.INVOICE_NOT_FOUND})
    @GetMapping(value = "/{code}/invoice", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable String code, @AuthenticationPrincipal Jwt jwt) {
        Invoice invoice = orders.loadInvoice(code, CurrentUser.from(jwt));
        return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=\"" + invoice.getFileName() + "\"")
                .body(invoice.getContent());
    }
}
