package com.fptaptech.catalog.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DailyRevenueReport(
        LocalDate from,
        LocalDate to,
        String currency,
        BigDecimal total,
        List<DailyRevenuePoint> days
) {
}
