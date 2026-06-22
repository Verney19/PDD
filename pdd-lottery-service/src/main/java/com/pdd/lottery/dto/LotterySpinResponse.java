package com.pdd.lottery.dto;

import java.io.Serializable;
import java.util.List;

public record LotterySpinResponse(
        Long activityId,
        Long userId,
        PrizeSlice prize,
        Integer prizeIndex,
        Boolean winner,
        String requestId,
        List<PrizeSlice> prizePool,
        String status
) implements Serializable {
}
