package com.fptaptech.catalog.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "BankTransferPayment", description = "Chuyển khoản ngân hàng",
        example = "{\"method\":\"BANK_TRANSFER\",\"bankCode\":\"VCB\",\"transferRef\":\"FT26016ABC\"}")
public record BankTransferPayment(
        @Schema(example = "BANK_TRANSFER", allowableValues = {"BANK_TRANSFER"})
        @NotBlank
        String method,
        @Schema(example = "VCB")
        @NotBlank
        String bankCode,
        @Schema(example = "FT26016ABC")
        @NotBlank
        String transferRef
) implements Payment {
}
