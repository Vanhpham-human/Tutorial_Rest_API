package com.fptaptech.catalog.common;

import org.springframework.http.HttpStatus;

import java.util.List;

public class CatalogException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final String title;
    private final List<ApiFieldError> errors;

    public CatalogException(HttpStatus status, String code, String title, String detail) {
        this(status, code, title, detail, List.of());
    }

    public CatalogException(HttpStatus status, String code, String title, String detail, List<ApiFieldError> errors) {
        super(detail);
        this.status = status;
        this.code = code;
        this.title = title;
        this.errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public List<ApiFieldError> getErrors() {
        return errors;
    }
}
