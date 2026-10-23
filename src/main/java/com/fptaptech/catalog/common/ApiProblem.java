package com.fptaptech.catalog.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@Schema(name = "ApiProblem", description = "RFC 9457 Problem Details, thêm code và errors")
public record ApiProblem(
        @Schema(example = "https://api.example.com/errors/validation") URI type,
        @Schema(example = "Validation failed") String title,
        @Schema(example = "400") int status,
        @Schema(example = "Invalid request content.") String detail,
        @Schema(example = "/api/orders") URI instance,
        @Schema(example = "VALIDATION_FAILED") String code,
        Instant timestamp,
        List<ApiFieldError> errors
) {
}
