package com.fptaptech.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiSpecTest {

    @Autowired
    MockMvc mvc;

    @Test
    void specDescribesProductApi() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi", startsWith("3.1")))
                .andExpect(jsonPath("$.info.title").value("Catalog API"))
                .andExpect(jsonPath("$.paths['/api/v1/products'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/products/{id}'].get.responses['404']").exists())
                .andExpect(jsonPath("$.components.schemas.ProductRequest.required",
                        containsInAnyOrder("name", "price")));
    }
}
