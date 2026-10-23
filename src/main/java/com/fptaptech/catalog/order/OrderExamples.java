package com.fptaptech.catalog.order;

public final class OrderExamples {

    public static final String COD = """
            {
              "shippingAddress": "12 Nguyen Hue, Quan 1, TP.HCM",
              "payment": { "method": "COD" },
              "items": [ { "productId": 1, "quantity": 1 } ]
            }
            """;

    public static final String CARD = """
            {
              "shippingAddress": "12 Nguyen Hue, Quan 1, TP.HCM",
              "payment": { "method": "CARD", "provider": "VISA", "last4": "4242" },
              "items": [
                { "productId": 1, "quantity": 2 },
                { "productId": 2, "quantity": 1 }
              ]
            }
            """;

    public static final String BANK_TRANSFER = """
            {
              "shippingAddress": "25 Le Loi, Hue",
              "payment": { "method": "BANK_TRANSFER", "bankCode": "VCB", "transferRef": "FT26016ABC" },
              "items": [ { "productId": 2, "quantity": 1 } ]
            }
            """;

    private OrderExamples() {
    }
}
