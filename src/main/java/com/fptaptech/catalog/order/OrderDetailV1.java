package com.fptaptech.catalog.order;

import com.fptaptech.catalog.payment.Payment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(name = "OrderDetailV1", description = "Chi tiết đơn phiên bản 1. totalAmount là số. Ngừng hỗ trợ 2027-04-01.")
public record OrderDetailV1(
        @Schema(example = "ORD-2026-000001") String code,
        Long customerId,
        OrderStatus status,
        String shippingAddress,
        @Schema(example = "1590000", description = "Tổng tiền dạng số. Deprecated cùng operation.")
        BigDecimal totalAmount,
        @Schema(example = "VND") String currency,
        @Schema(example = "0") Long version,
        Payment payment,
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant deliveredAt
) {
}
