package com.pdd.common.client.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ActivityResponse(
        Long id,
        Long productId,
        String productName,
        String type,
        BigDecimal activityPrice,
        Integer totalStock,
        Integer availableStock,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer status
) implements Serializable {
}
