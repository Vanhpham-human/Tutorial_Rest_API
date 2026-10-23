package com.fptaptech.catalog.payment;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "method", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CodPayment.class, name = "COD"),
        @JsonSubTypes.Type(value = CardPayment.class, name = "CARD"),
        @JsonSubTypes.Type(value = BankTransferPayment.class, name = "BANK_TRANSFER")
})
@Schema(
        name = "Payment",
        description = "Phương thức thanh toán. Chọn một trong ba kiểu theo method.",
        discriminatorProperty = "method",
        oneOf = {CodPayment.class, CardPayment.class, BankTransferPayment.class},
        discriminatorMapping = {
                @DiscriminatorMapping(value = "COD", schema = CodPayment.class),
                @DiscriminatorMapping(value = "CARD", schema = CardPayment.class),
                @DiscriminatorMapping(value = "BANK_TRANSFER", schema = BankTransferPayment.class)
        }
)
public sealed interface Payment permits CodPayment, CardPayment, BankTransferPayment {

    String method();
}
