package com.fptaptech.catalog.report;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record RevenueQuery(
        @Schema(description = "Từ ngày (UTC, inclusive). Mặc định 30 ngày gần nhất.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate from,
        @Schema(description = "Đến ngày (UTC, inclusive). Mặc định hôm nay.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate to
) {
}
