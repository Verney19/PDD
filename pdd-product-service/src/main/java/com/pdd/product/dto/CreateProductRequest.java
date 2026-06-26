package com.pdd.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 创建商品请求。
 */
public record CreateProductRequest(
        @NotBlank @Size(max = 128) String name,
        @NotNull @PositiveOrZero BigDecimal price,
        @NotNull @PositiveOrZero Integer totalStock,
        @NotNull @PositiveOrZero Integer availableStock,
        @NotBlank String category,
        @Size(max = 512) String description,
        @Size(max = 256) String imageUrl
) {
}
