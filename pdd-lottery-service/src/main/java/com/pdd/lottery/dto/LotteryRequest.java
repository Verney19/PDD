package com.pdd.lottery.dto;

import jakarta.validation.constraints.NotNull;

public record LotteryRequest(@NotNull Long activityId) {
}
