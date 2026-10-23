package com.fptaptech.catalog;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiContractTest {

    @Autowired
    MockMvc mvc;

    @Test
    void swaggerUiListsExactlyFourGroups() throws Exception {
        mvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urls.length()").value(4))
                .andExpect(jsonPath("$.urls[*].name", containsInAnyOrder("public-v1", "public-v2", "admin", "partner")));
    }

    @Test
    void publicV1IsDeprecatedAndNarrow() throws Exception {
        String body = spec("public-v1");
        assertThat(body).doesNotContain("PageImpl");
        assertThat(body).doesNotContain("/api/webhooks");
        assertThat(body).doesNotContain("/api/admin");
        mvc.perform(get("/v3/api-docs/public-v1"))
                .andExpect(jsonPath("$.paths['/api/orders'].post").exists())
                .andExpect(jsonPath("$.paths['/api/orders'].get.parameters[*].name",
                        containsInAnyOrder("page", "size", "sort", "status", "from", "to", "minTotal")))
                .andExpect(jsonPath("$.paths['/api/orders'].get.parameters[?(@.name=='sort')].schema.enum",
                        hasItem(containsInAnyOrder("createdAt,asc", "createdAt,desc", "totalAmount,asc", "totalAmount,desc"))))
                .andExpect(jsonPath("$.paths['/api/orders'].post.parameters[?(@.name=='Idempotency-Key')].in",
                        hasItem("header")))
                .andExpect(jsonPath("$.paths['/api/orders'].post.parameters[?(@.name=='Idempotency-Key')].required",
                        hasItem(true)))
                .andExpect(jsonPath("$.paths['/api/orders'].post.requestBody.content['application/json'].examples.COD").exists())
                .andExpect(jsonPath("$.paths['/api/orders'].post.requestBody.content['application/json'].examples.CARD").exists())
                .andExpect(jsonPath("$.paths['/api/orders'].post.responses['201'].headers['X-Request-Id']").exists())
                .andExpect(jsonPath("$.paths['/api/orders'].post.responses['409'].content['application/problem+json'].examples.IDEMPOTENCY_CONFLICT").exists())
                .andExpect(jsonPath("$.paths['/api/orders'].get.responses['404']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/orders/{code}'].get.deprecated").value(true))
                .andExpect(jsonPath("$.paths['/api/orders/{code}'].get.description", org.hamcrest.Matchers.containsString("2027-04-01")))
                .andExpect(jsonPath("$.paths['/api/orders/{code}'].get.responses['200'].content['application/json'].schema.$ref")
                        .value("#/components/schemas/OrderDetailV1"))
                .andExpect(jsonPath("$.components.schemas.OrderDetailV2").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.OrderAction.enum", containsInAnyOrder("CANCEL", "REQUEST_RETURN")))
                .andExpect(jsonPath("$.components.schemas.OrderAction.description", org.hamcrest.Matchers.containsString("CUSTOMER")))
                .andExpect(jsonPath("$.components.schemas.Payment.discriminator.propertyName").value("method"))
                .andExpect(jsonPath("$.components.schemas.Payment.discriminator.mapping.COD").exists())
                .andExpect(jsonPath("$.components.schemas.Payment.discriminator.mapping.CARD").exists())
                .andExpect(jsonPath("$.components.schemas.Payment.discriminator.mapping.BANK_TRANSFER").exists())
                .andExpect(jsonPath("$.components.schemas.Payment.oneOf").isArray())
                .andExpect(jsonPath("$.components.schemas.CodPayment.example.method").value("COD"))
                .andExpect(jsonPath("$.components.schemas.CardPayment.example.method").value("CARD"))
                .andExpect(jsonPath("$.components.schemas.BankTransferPayment.example.method").value("BANK_TRANSFER"))
                .andExpect(jsonPath("$.components.schemas.ApiProblem.properties.code").exists())
                .andExpect(jsonPath("$.components.schemas.ApiProblem.properties.errors").exists())
                .andExpect(jsonPath("$.components.schemas.OrderPage.properties.content").exists())
                .andExpect(jsonPath("$.components.schemas.OrderPage.properties.totalElements").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.partnerKey").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/orders'].post.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/orders/{code}/invoice'].get").exists())
                .andExpect(jsonPath("$.paths['/api/orders/{code}/invoice'].post").doesNotExist());
    }

    @Test
    void publicV2DocumentsMoneyAndIsNotDeprecated() throws Exception {
        mvc.perform(get("/v3/api-docs/public-v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/orders/{code}'].get.deprecated").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/orders/{code}'].get.responses['200'].content['application/json'].schema.$ref")
                        .value("#/components/schemas/OrderDetailV2"))
                .andExpect(jsonPath("$.components.schemas.OrderDetailV1").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.OrderDetailV2.properties.total").exists())
                .andExpect(jsonPath("$.components.schemas.Money.properties.value").exists())
                .andExpect(jsonPath("$.components.schemas.Money.properties.currency").exists());
    }

    @Test
    void adminAndPartnerStayInTheirGroups() throws Exception {
        String admin = spec("admin");
        assertThat(admin).contains("/api/orders/{code}/transitions");
        assertThat(admin).contains("/api/orders/{code}/invoice");
        assertThat(admin).contains("/api/admin/reports/daily-revenue");
        assertThat(admin).doesNotContain("/api/webhooks");
        mvc.perform(get("/v3/api-docs/admin"))
                .andExpect(jsonPath("$.components.schemas.OrderAction.enum",
                        containsInAnyOrder("CONFIRM", "CANCEL", "SHIP", "FAIL_DELIVERY", "DELIVER", "REQUEST_RETURN", "ACCEPT_RETURN")))
                .andExpect(jsonPath("$.components.schemas.OrderAction.description", org.hamcrest.Matchers.containsString("ADMIN")))
                .andExpect(jsonPath("$.paths['/api/orders/{code}/transitions'].post.parameters[?(@.name=='If-Match')].required",
                        hasItem(true)))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/orders'].get").doesNotExist());

        mvc.perform(get("/v3/api-docs/partner"))
                .andExpect(jsonPath("$.paths['/api/webhooks/shipping'].post.security[0].partnerKey").exists())
                .andExpect(jsonPath("$.components.securitySchemes.partnerKey.type").value("apiKey"))
                .andExpect(jsonPath("$.components.securitySchemes.partnerKey.in").value("header"))
                .andExpect(jsonPath("$.components.securitySchemes.partnerKey.name").value("X-Partner-Key"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.OrderAction.enum",
                        containsInAnyOrder("SHIP", "DELIVER", "FAIL_DELIVERY")))
                .andExpect(jsonPath("$.paths['/api/orders']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/webhooks/shipping'].post.responses['401'].content['application/problem+json'].examples.PARTNER_KEY_INVALID").exists());
    }

    private String spec(String group) throws Exception {
        return mvc.perform(get("/v3/api-docs/" + group))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
