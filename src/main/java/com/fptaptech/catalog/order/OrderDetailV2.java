package com.fptaptech.catalog.order;

import com.fptaptech.catalog.payment.Payment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(name = "OrderDetailV2", description = "Chi tiết đơn phiên bản 2. total là object {value, currency}.")
public record OrderDetailV2(
        @Schema(example = "ORD-2026-000001") String code,
        Long customerId,
        OrderStatus status,
        String shippingAddress,
        Money total,
        @Schema(example = "0") Long version,
        Payment payment,
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant deliveredAt
) {
}
