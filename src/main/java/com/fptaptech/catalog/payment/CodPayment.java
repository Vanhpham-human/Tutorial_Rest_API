package com.fptaptech.catalog.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "CodPayment", description = "Thanh toán khi nhận hàng", example = "{\"method\":\"COD\"}")
public record CodPayment(
        @Schema(example = "COD", allowableValues = {"COD"})
        @NotBlank
        String method
) implements Payment {
}
