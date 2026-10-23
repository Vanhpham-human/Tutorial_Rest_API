package com.fptaptech.catalog.order;

import com.fptaptech.catalog.payment.Payment;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Tạo đơn. Client không gửi giá; server chụp giá sản phẩm tại thời điểm đặt.")
public record CreateOrderRequest(
        @Schema(example = "12 Nguyen Hue, Quan 1, TP.HCM")
        @NotBlank
        @Size(max = 500)
        String shippingAddress,
        @NotNull
        @Valid
        Payment payment,
        @NotEmpty
        @Size(max = 50)
        @Valid
        List<OrderLineRequest> items
) {
}
