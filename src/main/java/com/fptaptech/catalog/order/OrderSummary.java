package com.fptaptech.catalog.order;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(name = "OrderSummary")
public record OrderSummary(
        @Schema(example = "ORD-2026-000001") String code,
        OrderStatus status,
        @Schema(example = "1590000") BigDecimal totalAmount,
        @Schema(example = "VND") String currency,
        Instant createdAt
) {
}
