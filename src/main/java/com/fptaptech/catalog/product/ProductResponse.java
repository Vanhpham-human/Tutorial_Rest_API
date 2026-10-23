package com.fptaptech.catalog.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Thông tin sản phẩm trả về")
public record ProductResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Bàn phím cơ K8 Pro") String name,
        String description,
        @Schema(example = "1590000") BigDecimal price,
        Instant createdAt,
        Instant updatedAt
) {
}
