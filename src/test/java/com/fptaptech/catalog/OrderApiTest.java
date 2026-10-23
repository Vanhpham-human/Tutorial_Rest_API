package com.fptaptech.catalog;

import com.fptaptech.catalog.customer.CustomerRepository;
import com.fptaptech.catalog.order.IdempotencyRepository;
import com.fptaptech.catalog.order.InvoiceRepository;
import com.fptaptech.catalog.order.OrderEventRepository;
import com.fptaptech.catalog.order.OrderItemRepository;
import com.fptaptech.catalog.order.OrderRepository;
import com.fptaptech.catalog.security.DevTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderApiTest {

    @Autowired MockMvc mvc;
    @Autowired DevTokenService tokens;
    @Autowired CustomerRepository customers;
    @Autowired OrderRepository orders;
    @Autowired OrderItemRepository items;
    @Autowired OrderEventRepository events;
    @Autowired InvoiceRepository invoices;
    @Autowired IdempotencyRepository idempotency;
    @Autowired PlatformTransactionManager transactionManager;
    @Value("${app.security.partner-key}") String partnerKey;

    private String lan;
    private String minh;
    private String admin;

    @BeforeEach
    void cleanAndAuthenticate() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            invoices.deleteAllInBatch();
            events.deleteAllInBatch();
            items.deleteAllInBatch();
            orders.deleteAllInBatch();
            idempotency.deleteAllInBatch();
        });
        lan = tokens.issueCustomer(customers.findByEmail("lan@example.com").orElseThrow());
        minh = tokens.issueCustomer(customers.findByEmail("minh@example.com").orElseThrow());
        admin = tokens.issueAdmin();
    }

    @Test
    void pricesComeFromTheCatalogAndCustomersOnlySeeTheirOrders() throws Exception {
        String code = create(lan, "lan-price", """
                {"shippingAddress":"12 Nguyen Hue","payment":{"method":"COD"},
                 "items":[{"productId":1,"quantity":2},{"productId":2,"quantity":1}],
                 "totalAmount":1}
                """);
        mvc.perform(get("/api/orders/{code}", code).header("Authorization", bearer(lan)).header("X-API-Version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(3970000.0))
                .andExpect(jsonPath("$.currency").value("VND"))
                .andExpect(header().exists("ETag"));

        mvc.perform(get("/api/orders/{code}", code).header("Authorization", bearer(lan)).header("X-API-Version", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total.value").value(3970000.0))
                .andExpect(jsonPath("$.total.currency").value("VND"))
                .andExpect(jsonPath("$.totalAmount").doesNotExist());

        mvc.perform(get("/api/orders/{code}", code).header("Authorization", bearer(minh)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));

        mvc.perform(get("/api/orders").header("Authorization", bearer(minh)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/orders").header("Authorization", bearer(admin)).param("sort", "totalAmount,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value(code));
    }

    @Test
    void walksValidTransitionsAndRejectsInvalidOnes() throws Exception {
        String code = create(lan, "lan-flow", """
                {"shippingAddress":"12 Nguyen Hue","payment":{"method":"CARD","provider":"VISA","last4":"4242"},
                 "items":[{"productId":1,"quantity":1}]}
                """);
        MvcResult created = mvc.perform(get("/api/orders/{code}", code).header("Authorization", bearer(lan)))
                .andExpect(status().isOk()).andReturn();
        String etag = created.getResponse().getHeader("ETag");

        mvc.perform(post("/api/orders/{code}/transitions", code)
                        .header("Authorization", bearer(lan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"CONFIRM"}
                                """))
                .andExpect(status().isPreconditionRequired())
                .andExpect(jsonPath("$.code").value("PRECONDITION_REQUIRED"));

        mvc.perform(post("/api/orders/{code}/transitions", code)
                        .header("Authorization", bearer(lan))
                        .header("If-Match", etag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"CONFIRM"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        etag = transition(admin, code, etag, "CONFIRM", null);
        mvc.perform(post("/api/orders/{code}/transitions", code)
                        .header("Authorization", bearer(admin))
                        .header("If-Match", "\"999\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"SHIP"}
                                """))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.code").value("PRECONDITION_FAILED"));

        mvc.perform(post("/api/orders/{code}/transitions", code)
                        .header("Authorization", bearer(admin))
                        .header("If-Match", etag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"CANCEL"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REASON_REQUIRED"));

        etag = transition(admin, code, etag, "CANCEL", "Khách đổi ý");
        mvc.perform(get("/api/orders/{code}", code).header("Authorization", bearer(admin)).header("X-API-Version", "2"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        String second = create(lan, "lan-ship", """
                {"shippingAddress":"25 Le Loi","payment":{"method":"BANK_TRANSFER","bankCode":"VCB","transferRef":"FT1"},
                 "items":[{"productId":2,"quantity":1}]}
                """);
        String secondTag = etagOf(second, lan);
        secondTag = transition(admin, second, secondTag, "CONFIRM", null);
        mvc.perform(post("/api/webhooks/shipping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderCode":"%s","action":"SHIP"}
                                """.formatted(second)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/webhooks/shipping")
                        .header("X-Partner-Key", partnerKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderCode":"%s","action":"DELIVER"}
                                """.formatted(second)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_INVALID_TRANSITION"));
        mvc.perform(post("/api/webhooks/shipping")
                        .header("X-Partner-Key", partnerKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderCode":"%s","action":"SHIP"}
                                """.formatted(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPING"));
        mvc.perform(post("/api/webhooks/shipping")
                        .header("X-Partner-Key", partnerKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderCode":"%s","action":"FAIL_DELIVERY","reason":"Vắng nhà"}
                                """.formatted(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
        mvc.perform(post("/api/webhooks/shipping")
                        .header("X-Partner-Key", partnerKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderCode":"%s","action":"SHIP"}
                                """.formatted(second)))
                .andExpect(jsonPath("$.status").value("SHIPPING"));
        mvc.perform(post("/api/webhooks/shipping")
                        .header("X-Partner-Key", partnerKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderCode":"%s","action":"DELIVER"}
                                """.formatted(second)))
                .andExpect(jsonPath("$.status").value("DELIVERED"));

        String deliveredTag = etagOf(second, lan);
        mvc.perform(post("/api/orders/{code}/transitions", second)
                        .header("Authorization", bearer(minh))
                        .header("If-Match", deliveredTag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"REQUEST_RETURN","reason":"Lỗi"}
                                """))
                .andExpect(status().isNotFound());
        transition(lan, second, deliveredTag, "REQUEST_RETURN", "Lỗi phím");
        String returnTag = etagOf(second, admin);
        transition(admin, second, returnTag, "ACCEPT_RETURN", null);
        mvc.perform(get("/api/orders/{code}", second).header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.status").value("RETURNED"));
    }

    @Test
    void returnWindowExpiresAfterSevenDays() throws Exception {
        String code = create(lan, "lan-return", """
                {"shippingAddress":"12 Nguyen Hue","payment":{"method":"COD"},"items":[{"productId":1,"quantity":1}]}
                """);
        String etag = etagOf(code, lan);
        etag = transition(admin, code, etag, "CONFIRM", null);
        transition(admin, code, etag, "SHIP", null);
        mvc.perform(post("/api/webhooks/shipping")
                        .header("X-Partner-Key", partnerKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderCode":"%s","action":"DELIVER"}
                                """.formatted(code)))
                .andExpect(status().isOk());
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var order = orders.findByCode(code).orElseThrow();
            order.setDeliveredAt(Instant.now().minus(8, ChronoUnit.DAYS));
            orders.save(order);
        });
        mvc.perform(post("/api/orders/{code}/transitions", code)
                        .header("Authorization", bearer(lan))
                        .header("If-Match", etagOf(code, lan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"REQUEST_RETURN","reason":"Muộn"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RETURN_WINDOW_EXPIRED"));
    }

    @Test
    void idempotencyReplaysAndConflicts() throws Exception {
        String body = """
                {"shippingAddress":"12 Nguyen Hue","payment":{"method":"COD"},"items":[{"productId":1,"quantity":1}]}
                """;
        MvcResult first = mvc.perform(post("/api/orders")
                        .header("Authorization", bearer(lan))
                        .header("Idempotency-Key", "same-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();
        mvc.perform(post("/api/orders")
                        .header("Authorization", bearer(lan))
                        .header("Idempotency-Key", "same-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(json(first, "code")));
        assertThat(orders.count()).isEqualTo(1);

        mvc.perform(post("/api/orders")
                        .header("Authorization", bearer(lan))
                        .header("Idempotency-Key", "same-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shippingAddress":"Khác","payment":{"method":"COD"},"items":[{"productId":2,"quantity":1}]}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));

        mvc.perform(post("/api/orders")
                        .header("Authorization", bearer(lan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REQUIRED"));

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var record = idempotency.findAll().getFirst();
            record.setCreatedAt(Instant.now().minus(25, ChronoUnit.HOURS));
            idempotency.save(record);
        });
        mvc.perform(post("/api/orders")
                        .header("Authorization", bearer(lan))
                        .header("Idempotency-Key", "same-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shippingAddress":"Khác","payment":{"method":"COD"},"items":[{"productId":2,"quantity":1}]}
                                """))
                .andExpect(status().isCreated());
        assertThat(orders.count()).isEqualTo(2);
    }

    @Test
    void rejectsDuplicateLinesAndBadSort() throws Exception {
        mvc.perform(post("/api/orders")
                        .header("Authorization", bearer(lan))
                        .header("Idempotency-Key", "dup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shippingAddress":"12 Nguyen Hue","payment":{"method":"COD"},
                                 "items":[{"productId":1,"quantity":1},{"productId":1,"quantity":2}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DUPLICATE_PRODUCT"));
        mvc.perform(get("/api/orders").header("Authorization", bearer(lan)).param("sort", "name,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SORT"));
        mvc.perform(post("/api/orders")
                        .header("Authorization", bearer(lan))
                        .header("Idempotency-Key", "missing-product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shippingAddress":"12 Nguyen Hue","payment":{"method":"COD"},"items":[{"productId":99,"quantity":1}]}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void invoiceIsPdfOnlyAndRevenueCountsDeliveredOrders() throws Exception {
        String code = create(lan, "lan-invoice", """
                {"shippingAddress":"12 Nguyen Hue","payment":{"method":"COD"},"items":[{"productId":1,"quantity":1}]}
                """);
        String etag = etagOf(code, admin);
        etag = transition(admin, code, etag, "CONFIRM", null);
        transition(admin, code, etag, "SHIP", null);
        mvc.perform(post("/api/webhooks/shipping")
                        .header("X-Partner-Key", partnerKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderCode":"%s","action":"DELIVER"}
                                """.formatted(code)))
                .andExpect(status().isOk());

        byte[] pdf = "%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF".getBytes();
        mvc.perform(multipart("/api/orders/{code}/invoice", code)
                        .file(new MockMultipartFile("file", "invoice.pdf", "text/plain", pdf))
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(multipart("/api/orders/{code}/invoice", code)
                        .file(new MockMultipartFile("file", "invoice.pdf", "application/pdf", pdf))
                        .header("Authorization", bearer(lan)))
                .andExpect(status().isForbidden());
        mvc.perform(multipart("/api/orders/{code}/invoice", code)
                        .file(new MockMultipartFile("file", "invoice.pdf", "application/pdf", pdf))
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderCode").value(code));
        mvc.perform(get("/api/orders/{code}/invoice", code).header("Authorization", bearer(lan)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/pdf")))
                .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray()).startsWith("%PDF".getBytes()));
        mvc.perform(get("/api/orders/{code}/invoice", code).header("Authorization", bearer(minh)))
                .andExpect(status().isNotFound());

        String day = orders.findByCode(code).orElseThrow().getDeliveredAt().toString().substring(0, 10);
        mvc.perform(get("/api/admin/reports/daily-revenue")
                        .header("Authorization", bearer(admin))
                        .param("from", day)
                        .param("to", day))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1590000.0))
                .andExpect(jsonPath("$.days[0].orderCount").value(1));
        mvc.perform(get("/api/admin/reports/daily-revenue").header("Authorization", bearer(lan)))
                .andExpect(status().isForbidden());
    }

    private String create(String token, String key, String json) throws Exception {
        MvcResult result = mvc.perform(post("/api/orders")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Request-Id"))
                .andReturn();
        return json(result, "code");
    }

    private String transition(String token, String code, String etag, String action, String reason) throws Exception {
        String payload = reason == null
                ? "{\"action\":\"%s\"}".formatted(action)
                : "{\"action\":\"%s\",\"reason\":\"%s\"}".formatted(action, reason);
        MvcResult result = mvc.perform(post("/api/orders/{code}/transitions", code)
                        .header("Authorization", bearer(token))
                        .header("If-Match", etag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getHeader("ETag");
    }

    private String etagOf(String code, String token) throws Exception {
        return mvc.perform(get("/api/orders/{code}", code).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader("ETag");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String json(MvcResult result, String field) throws Exception {
        String body = result.getResponse().getContentAsString();
        String marker = "\"" + field + "\":\"";
        int start = body.indexOf(marker) + marker.length();
        return body.substring(start, body.indexOf('"', start));
    }
}
