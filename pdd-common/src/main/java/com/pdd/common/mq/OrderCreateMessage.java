package com.pdd.common.mq;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 下单消息体。
 * <p>
 * 秒杀和抽奖服务都通过它把“建单请求”投递到订单服务。
 */
public record OrderCreateMessage(
        String requestId,
        Long userId,
        Long activityId,
        Long productId,
        Integer quantity,
        BigDecimal amount,
        String source,
        LocalDateTime createdAt
) implements Serializable {
}
