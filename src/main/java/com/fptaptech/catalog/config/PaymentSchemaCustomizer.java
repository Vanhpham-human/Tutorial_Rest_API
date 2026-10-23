package com.fptaptech.catalog.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Order(10)
public class PaymentSchemaCustomizer implements GlobalOpenApiCustomizer {

    @Override
    public void customise(OpenAPI openApi) {
        if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
            return;
        }
        var schemas = openApi.getComponents().getSchemas();
        if (!schemas.containsKey("CodPayment")) {
            return;
        }
        example(schemas.get("CodPayment"), Map.of("method", "COD"));
        example(schemas.get("CardPayment"), Map.of("method", "CARD", "provider", "VISA", "last4", "4242"));
        example(schemas.get("BankTransferPayment"), Map.of(
                "method", "BANK_TRANSFER", "bankCode", "VCB", "transferRef", "FT26016ABC"));
        detachFromPayment(schemas.get("CodPayment"));
        detachFromPayment(schemas.get("CardPayment"));
        detachFromPayment(schemas.get("BankTransferPayment"));
        Schema<?> payment = new Schema<>();
        payment.setDescription("Phương thức thanh toán. oneOf theo discriminator method.");
        payment.setOneOf(List.of(
                ref("CodPayment"),
                ref("CardPayment"),
                ref("BankTransferPayment")));
        Discriminator discriminator = new Discriminator().propertyName("method");
        discriminator.setMapping(Map.of(
                "COD", "#/components/schemas/CodPayment",
                "CARD", "#/components/schemas/CardPayment",
                "BANK_TRANSFER", "#/components/schemas/BankTransferPayment"));
        payment.setDiscriminator(discriminator);
        schemas.put("Payment", payment);
        Schema<?> create = schemas.get("CreateOrderRequest");
        if (create != null && create.getProperties() != null) {
            create.getProperties().put("payment", ref("Payment"));
        }
    }

    private static void detachFromPayment(Schema<?> schema) {
        if (schema == null || schema.getAllOf() == null) {
            return;
        }
        Schema<?> inline = null;
        for (Schema<?> part : schema.getAllOf()) {
            if (part.get$ref() == null) {
                inline = part;
            }
        }
        schema.setAllOf(null);
        if (schema.getType() == null) {
            schema.setType("object");
        }
        if (inline != null && schema.getProperties() == null && inline.getProperties() != null) {
            schema.setProperties(inline.getProperties());
        }
    }

    private static void example(Schema<?> schema, Map<String, String> value) {
        if (schema != null && schema.getExample() == null) {
            schema.setExample(value);
        }
    }

    private static Schema<?> ref(String name) {
        Schema<?> schema = new Schema<>();
        schema.set$ref("#/components/schemas/" + name);
        return schema;
    }
}
