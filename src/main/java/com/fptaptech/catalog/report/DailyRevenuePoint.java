package com.fptaptech.catalog.report;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyRevenuePoint(
        LocalDate date,
        @Schema(example = "1590000") BigDecimal amount,
        @Schema(example = "1") long orderCount
) {
}
