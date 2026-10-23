package com.fptaptech.catalog.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Dữ liệu tạo hoặc cập nhật sản phẩm")
public record ProductRequest(
        @Schema(description = "Tên sản phẩm", example = "Bàn phím cơ K8 Pro")
        @NotBlank @Size(max = 120)
        String name,
        @Schema(description = "Mô tả ngắn", example = "Switch Gateron Brown, layout 87 phím")
        @Size(max = 500)
        String description,
        @Schema(description = "Giá bán (VND)", example = "1590000")
        @NotNull @DecimalMin("0.0") @Digits(integer = 10, fraction = 2)
        BigDecimal price
) {
}
