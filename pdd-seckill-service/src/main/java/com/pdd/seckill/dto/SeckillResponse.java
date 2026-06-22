package com.pdd.seckill.dto;

public record SeckillResponse(
        String requestId,
        Long remainingStock,
        String status
) {
}
