package com.fptaptech.catalog.order;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "OrderPage", description = "Trang đơn hàng với content, page, size và totalElements.")
public record OrderPage(
        List<OrderSummary> content,
        @Schema(example = "0") int page,
        @Schema(example = "20") int size,
        @Schema(example = "1") long totalElements,
        @Schema(example = "1") int totalPages,
        boolean first,
        boolean last
) {
}
