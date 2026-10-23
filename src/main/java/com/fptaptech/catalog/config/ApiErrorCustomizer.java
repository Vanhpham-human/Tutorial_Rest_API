package com.fptaptech.catalog.config;

import com.fptaptech.catalog.common.ApiErrorCode;
import com.fptaptech.catalog.common.ApiErrorResponses;
import com.fptaptech.catalog.common.ApiProblem;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Order(0)
public class ApiErrorCustomizer implements GlobalOperationCustomizer, GlobalOpenApiCustomizer {

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        ApiErrorResponses declared = handlerMethod.getMethodAnnotation(ApiErrorResponses.class);
        if (declared == null) {
            return operation;
        }
        Map<Integer, ApiResponse> byStatus = new LinkedHashMap<>();
        for (ApiErrorCode code : declared.value()) {
            ApiResponse response = byStatus.computeIfAbsent(code.status(), status -> newResponse());
            Example example = new Example();
            example.setSummary(code.title());
            example.setValue(code.example());
            response.getContent().get("application/problem+json").addExamples(code.name(), example);
            String description = byStatus.get(code.status()).getDescription();
            String line = code.name() + ": " + code.title();
            response.setDescription(description == null || description.isBlank() ? line : description + "; " + line);
        }
        if (operation.getResponses() == null) {
            operation.setResponses(new io.swagger.v3.oas.models.responses.ApiResponses());
        }
        byStatus.forEach((status, response) -> operation.getResponses().addApiResponse(Integer.toString(status), response));
        return operation;
    }

    @Override
    public void customise(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        var resolved = ModelConverters.getInstance().readAllAsResolvedSchema(ApiProblem.class);
        if (resolved != null && resolved.schema != null) {
            openApi.getComponents().addSchemas("ApiProblem", resolved.schema);
            if (resolved.referencedSchemas != null) {
                resolved.referencedSchemas.forEach(openApi.getComponents()::addSchemas);
            }
        }
    }

    private static ApiResponse newResponse() {
        Schema<?> schema = new Schema<>();
        schema.set$ref("#/components/schemas/ApiProblem");
        MediaType mediaType = new MediaType().schema(schema);
        return new ApiResponse().description("").content(new Content().addMediaType("application/problem+json", mediaType));
    }
}
