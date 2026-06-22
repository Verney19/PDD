package com.pdd.lottery.dto;

public record LotteryJoinResponse(
        Long activityId,
        Long userId,
        Long participantCount,
        String status
) {
}
