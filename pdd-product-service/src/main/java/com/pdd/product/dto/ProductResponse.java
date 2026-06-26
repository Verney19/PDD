package com.pdd.product.dto;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        BigDecimal price,
        Integer totalStock,
        Integer availableStock,
        String category,
        String description,
        String imageUrl,
        Integer status
) {
}
