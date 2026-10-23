package com.fptaptech.catalog.config;

import com.fptaptech.catalog.common.ApiGroups;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiGroupsConfig {

    @Bean
    GroupedOpenApi publicV1(OpenApiGroupTuner tuner,
                            @Value("${app.api.server-url:http://localhost:8080}") String serverUrl,
                            @Value("${app.api.version:1.0.0}") String version,
                            @Value("${app.api.v1-sunset:2027-04-01}") String sunset) {
        return group("public-v1", tuner, serverUrl, version, sunset, "/api/orders", "/api/orders/**");
    }

    @Bean
    GroupedOpenApi publicV2(OpenApiGroupTuner tuner,
                            @Value("${app.api.server-url:http://localhost:8080}") String serverUrl,
                            @Value("${app.api.version:1.0.0}") String version,
                            @Value("${app.api.v1-sunset:2027-04-01}") String sunset) {
        return group("public-v2", tuner, serverUrl, version, sunset, "/api/orders", "/api/orders/**");
    }

    @Bean
    GroupedOpenApi admin(OpenApiGroupTuner tuner,
                         @Value("${app.api.server-url:http://localhost:8080}") String serverUrl,
                         @Value("${app.api.version:1.0.0}") String version,
                         @Value("${app.api.v1-sunset:2027-04-01}") String sunset) {
        return group("admin", tuner, serverUrl, version, sunset,
                "/api/orders", "/api/orders/**", "/api/admin/**");
    }

    @Bean
    GroupedOpenApi partner(OpenApiGroupTuner tuner,
                           @Value("${app.api.server-url:http://localhost:8080}") String serverUrl,
                           @Value("${app.api.version:1.0.0}") String version,
                           @Value("${app.api.v1-sunset:2027-04-01}") String sunset) {
        return group("partner", tuner, serverUrl, version, sunset, "/api/webhooks/**");
    }

    private static GroupedOpenApi group(String name, OpenApiGroupTuner tuner, String serverUrl, String version,
                                        String sunset, String... paths) {
        return GroupedOpenApi.builder()
                .group(name)
                .pathsToMatch(paths)
                .addOpenApiMethodFilter(method -> ApiGroups.Filter.matches(method, name))
                .addOpenApiCustomizer(openApi -> tuner.tune(openApi, name, serverUrl, version, sunset))
                .build();
    }
}
