package com.pdd.lottery.dto;

public record LotteryDrawResponse(
        Long activityId,
        Long participantCount,
        Integer winnerCount,
        String status
) {
}
