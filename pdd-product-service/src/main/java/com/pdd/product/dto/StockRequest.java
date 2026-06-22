package com.pdd.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockRequest(
        @NotNull Long activityId,
        @NotNull @Min(1) Integer quantity
) {
}
