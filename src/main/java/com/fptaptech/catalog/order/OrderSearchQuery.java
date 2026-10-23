package com.fptaptech.catalog.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OrderSearchQuery(
        @Schema(description = "Chỉ số trang, bắt đầu từ 0", defaultValue = "0")
        @Min(0)
        Integer page,
        @Schema(description = "Kích thước trang, tối đa 100", defaultValue = "20")
        @Min(1)
        @Max(100)
        Integer size,
        @Schema(description = "createdAt hoặc totalAmount, kèm asc hoặc desc",
                allowableValues = {"createdAt,asc", "createdAt,desc", "totalAmount,asc", "totalAmount,desc"},
                defaultValue = "createdAt,desc")
        String sort,
        @Schema(description = "Lọc theo trạng thái")
        OrderStatus status,
        @Schema(description = "Từ ngày tạo (UTC, inclusive), YYYY-MM-DD")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate from,
        @Schema(description = "Đến ngày tạo (UTC, inclusive), YYYY-MM-DD")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate to,
        @Schema(description = "Tổng tiền tối thiểu")
        @DecimalMin("0.0")
        BigDecimal minTotal
) {
}
