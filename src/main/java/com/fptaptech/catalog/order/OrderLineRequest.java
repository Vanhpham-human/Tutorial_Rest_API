package com.fptaptech.catalog.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderLineRequest(
        @Schema(example = "1")
        @NotNull
        Long productId,
        @Schema(example = "1", minimum = "1", maximum = "20")
        @NotNull
        @Min(1)
        @Max(20)
        Integer quantity
) {
}
