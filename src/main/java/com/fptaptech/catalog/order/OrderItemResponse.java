package com.fptaptech.catalog.order;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "OrderItemResponse")
public record OrderItemResponse(
        @Schema(example = "1") Long productId,
        @Schema(example = "Bàn phím cơ K8 Pro") String productName,
        @Schema(example = "1590000") BigDecimal unitPrice,
        @Schema(example = "1") int quantity,
        @Schema(example = "1590000") BigDecimal lineTotal
) {
}
