package com.fptaptech.catalog.webhook;

import com.fptaptech.catalog.common.ApiErrorCode;
import com.fptaptech.catalog.common.ApiErrorResponses;
import com.fptaptech.catalog.common.ApiGroups;
import com.fptaptech.catalog.common.Etags;
import com.fptaptech.catalog.order.OrderService;
import com.fptaptech.catalog.order.TransitionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Shipping", description = "Webhook đối tác vận chuyển")
@SecurityRequirement(name = "partnerKey")
@RequestMapping(path = "/api/webhooks", produces = MediaType.APPLICATION_JSON_VALUE)
public class ShippingWebhookController {

    private final OrderService orders;

    public ShippingWebhookController(OrderService orders) {
        this.orders = orders;
    }

    @ApiGroups({"partner"})
    @Operation(operationId = "shippingWebhook", summary = "Đối tác báo vận chuyển",
            description = "X-Partner-Key. SHIP khi đơn CONFIRMED, DELIVER hoặc FAIL_DELIVERY khi đơn SHIPPING. FAIL_DELIVERY đưa đơn về CONFIRMED để giao lại.")
    @ApiResponse(responseCode = "200", description = "Đã ghi nhận",
            content = @Content(schema = @Schema(implementation = TransitionResponse.class)))
    @ApiErrorResponses({
            ApiErrorCode.MALFORMED_JSON, ApiErrorCode.VALIDATION_FAILED, ApiErrorCode.PARTNER_KEY_INVALID,
            ApiErrorCode.ORDER_NOT_FOUND, ApiErrorCode.ORDER_INVALID_TRANSITION
    })
    @PostMapping("/shipping")
    public ResponseEntity<TransitionResponse> shipping(@Valid @RequestBody ShippingWebhookRequest request) {
        TransitionResponse body = orders.partner(request.orderCode(), request.action(), request.reason());
        return ResponseEntity.ok().eTag(Etags.format(body.version())).body(body);
    }
}
