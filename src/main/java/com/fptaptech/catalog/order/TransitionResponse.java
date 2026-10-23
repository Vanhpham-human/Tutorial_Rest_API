package com.fptaptech.catalog.order;

import io.swagger.v3.oas.annotations.media.Schema;

public record TransitionResponse(
        @Schema(example = "ORD-2026-000001") String code,
        OrderStatus status,
        @Schema(example = "1") Long version
) {
}
