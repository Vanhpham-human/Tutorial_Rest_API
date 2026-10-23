package com.fptaptech.catalog.common;

import org.springframework.http.HttpStatus;

public final class Etags {

    private Etags() {
    }

    public static String format(long version) {
        return "\"" + version + "\"";
    }

    public static long parse(String header) {
        if (header == null || header.isBlank()) {
            throw new CatalogException(
                    HttpStatus.PRECONDITION_REQUIRED,
                    "PRECONDITION_REQUIRED",
                    "Precondition required",
                    "Header If-Match là bắt buộc");
        }
        String value = header.trim();
        if (value.startsWith("W/")) {
            value = value.substring(2).trim();
        }
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new CatalogException(
                    HttpStatus.PRECONDITION_FAILED,
                    "PRECONDITION_FAILED",
                    "Precondition failed",
                    "If-Match không phải version hợp lệ");
        }
    }
}
