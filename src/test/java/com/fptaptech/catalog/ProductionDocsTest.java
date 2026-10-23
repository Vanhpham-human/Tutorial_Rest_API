package com.fptaptech.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class ProductionDocsTest {

    @Autowired
    MockMvc mvc;

    @Value("${app.security.partner-key}")
    String partnerKey;

    @Test
    void swaggerIsOffAndPartnerDocsRequireTheKey() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().isNotFound());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
        mvc.perform(get("/v3/api-docs/partner")).andExpect(status().isUnauthorized());
        mvc.perform(get("/v3/api-docs.yaml/partner")).andExpect(status().isUnauthorized());
        mvc.perform(get("/v3/api-docs/partner").header("X-Partner-Key", partnerKey)).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs/public-v2")).andExpect(status().isOk());
        mvc.perform(post("/api/dev/tokens").contentType("application/json").content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isNotFound());
    }
}
