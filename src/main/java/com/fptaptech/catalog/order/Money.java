package com.fptaptech.catalog.order;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "Money", description = "Số tiền kèm đơn vị")
public record Money(
        @Schema(example = "1590000") BigDecimal value,
        @Schema(example = "VND") String currency
) {
}
