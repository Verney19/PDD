package com.pdd.lottery.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public record LotteryWinnerFeedItem(
        Long activityId,
        Long userId,
        String userLabel,
        String prizeName,
        String level,
        LocalDateTime winTime
) implements Serializable {
}
