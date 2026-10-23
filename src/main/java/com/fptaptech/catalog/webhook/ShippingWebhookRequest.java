package com.fptaptech.catalog.webhook;

import com.fptaptech.catalog.order.OrderAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ShippingWebhookRequest(
        @Schema(example = "ORD-2026-000001")
        @NotBlank
        String orderCode,
        @Schema(description = "SHIP (đang giao), DELIVER (đã giao) hoặc FAIL_DELIVERY (giao thất bại, giao lại)")
        @NotNull
        OrderAction action,
        @Size(max = 500)
        String reason
) {
}
