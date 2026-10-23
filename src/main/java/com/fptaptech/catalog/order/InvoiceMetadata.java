package com.fptaptech.catalog.order;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record InvoiceMetadata(
        @Schema(example = "ORD-2026-000001") String orderCode,
        @Schema(example = "invoice.pdf") String fileName,
        @Schema(example = "1024") long sizeBytes,
        Instant uploadedAt
) {
}
