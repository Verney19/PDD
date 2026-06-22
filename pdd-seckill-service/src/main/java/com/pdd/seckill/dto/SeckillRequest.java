package com.pdd.seckill.dto;

import jakarta.validation.constraints.NotNull;

public record SeckillRequest(@NotNull Long activityId) {
}
