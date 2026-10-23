package com.fptaptech.catalog.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransitionRequest(
        @NotNull
        OrderAction action,
        @Schema(description = "Bắt buộc khi ADMIN hủy đơn đang CONFIRMED", example = "Khách đổi ý")
        @Size(max = 500)
        String reason
) {
}
