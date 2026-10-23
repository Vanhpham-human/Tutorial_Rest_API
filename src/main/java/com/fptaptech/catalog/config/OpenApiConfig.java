package com.fptaptech.catalog.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI catalogOpenApi(@Value("${app.api.version:1.0.0}") String version,
                           @Value("${app.api.server-url:http://localhost:8080}") String serverUrl) {
        return new OpenAPI()
                .info(new Info()
                        .title("Catalog API")
                        .version(version)
                        .description("REST API quản lý danh mục sản phẩm và đơn hàng TechShop")
                        .contact(new Contact().name("Catalog Team").email("catalog@example.com"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(new Server().url(serverUrl).description("Môi trường hiện tại")))
                .externalDocs(new ExternalDocumentation()
                        .description("Hướng dẫn tích hợp")
                        .url("https://example.com/docs/catalog"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT HS256. Claim role là CUSTOMER hoặc ADMIN. CUSTOMER kèm customerId."))
                        .addSecuritySchemes("partnerKey", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Partner-Key")
                                .description("API key của đối tác vận chuyển")));
    }
}
