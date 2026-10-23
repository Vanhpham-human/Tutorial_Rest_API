package com.fptaptech.catalog.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(name = "CardPayment", description = "Thanh toán thẻ",
        example = "{\"method\":\"CARD\",\"provider\":\"VISA\",\"last4\":\"4242\"}")
public record CardPayment(
        @Schema(example = "CARD", allowableValues = {"CARD"})
        @NotBlank
        String method,
        @Schema(example = "VISA", description = "Tổ chức thẻ")
        @NotBlank
        String provider,
        @Schema(example = "4242", description = "4 số cuối của thẻ")
        @NotBlank
        @Pattern(regexp = "\\d{4}")
        String last4
) implements Payment {
}
