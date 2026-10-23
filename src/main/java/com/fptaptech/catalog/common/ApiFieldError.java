package com.fptaptech.catalog.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiFieldError", description = "Một lỗi gắn với trường của request")
public record ApiFieldError(
        @Schema(example = "name") String field,
        @Schema(example = "must not be blank") String message
) {
}
