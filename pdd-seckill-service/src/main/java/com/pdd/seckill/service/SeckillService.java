package com.pdd.seckill.service;

import com.pdd.common.api.ErrorCode;
import com.pdd.common.api.Result;
import com.pdd.common.client.ActivityClient;
import com.pdd.common.client.dto.ActivityResponse;
import com.pdd.common.exception.BizException;
import com.pdd.common.model.ActivityType;
import com.pdd.common.model.OrderSource;
import com.pdd.common.mq.MqConstants;
import com.pdd.common.mq.OrderCreateMessage;
import com.pdd.common.redis.RedisKeys;
import com.pdd.seckill.dto.SeckillResponse;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 秒杀服务核心业务。
 * <p>
 * 通过 Redis Lua 脚本实现库存扣减与一人一单校验原子化，
 * 成功后再投递 RabbitMQ，由订单服务异步建单。
 */
@Service
public class SeckillService {
    private final ActivityClient activityClient;
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> seckillScript;
    private final DefaultRedisScript<Long> seckillReleaseScript;
    private final RabbitTemplate rabbitTemplate;

    /**
     * 注入活动 Feign 客户端、Redis、Lua 脚本和 RabbitMQ 模板。
     */
    public SeckillService(ActivityClient activityClient,
                          StringRedisTemplate redisTemplate,
                          @Qualifier("seckillScript") DefaultRedisScript<Long> seckillScript,
                          @Qualifier("seckillReleaseScript") DefaultRedisScript<Long> seckillReleaseScript,
                          RabbitTemplate rabbitTemplate) {
        this.activityClient = activityClient;
        this.redisTemplate = redisTemplate;
        this.seckillScript = seckillScript;
        this.seckillReleaseScript = seckillReleaseScript;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 执行一次秒杀请求。
     * <p>
     * 详细流程：
     * 1. 从商品活动服务读取活动信息
     * 2. 校验活动是否为秒杀、是否启用、是否在时间范围内
     * 3. 确认 Redis 中存在秒杀库存 key
     * 4. 执行 Lua 脚本，原子完成库存扣减和一人一单判断
     * 5. Redis 扣减成功后发送 RabbitMQ 下单消息
     * 6. 返回 QUEUED，表示请求已经进入异步下单队列
     */
    public SeckillResponse seckill(Long userId, Long activityId, boolean adminPreview) {
        ActivityResponse activity = loadActivity(activityId);
        validateActivity(activity, adminPreview);
        ensureStockKey(activity);

        String stockKey = RedisKeys.SECKILL_STOCK + activityId;
        String userKey = RedisKeys.SECKILL_USER + activityId;
        String requestId = OrderSource.SECKILL.name().toLowerCase() + ":" + activityId + ":" + userId;

        Long remaining = redisTemplate.execute(
                seckillScript,
                List.of(stockKey, userKey),
                String.valueOf(userId)
        );

        // Lua 脚本返回约定：
        // -1 未预热；-2 重复参与；-3 库存不足；>=0 为成功后的剩余库存。
        if (remaining == null) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "秒杀脚本执行失败");
        }
        if (remaining == -1L) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "秒杀库存未预热");
        }
        if (remaining == -2L) {
            throw new BizException(ErrorCode.DUPLICATE_PARTICIPATION, "你已经抢购过该商品");
        }
        if (remaining == -3L) {
            throw new BizException(ErrorCode.STOCK_NOT_ENOUGH);
        }

        OrderCreateMessage message = new OrderCreateMessage(
                requestId,
                userId,
                activity.id(),
                activity.productId(),
                1,
                activity.activityPrice(),
                OrderSource.SECKILL.name(),
                LocalDateTime.now()
        );
        try {
            // 秒杀接口只做快速接入，真正落库由订单服务异步完成。
            rabbitTemplate.convertAndSend(MqConstants.ORDER_EXCHANGE, MqConstants.SECKILL_ORDER_ROUTING_KEY, message);
        } catch (RuntimeException ex) {
            // 如果消息投递失败，需要把 Redis 中刚刚扣掉的库存和用户标记恢复。
            redisTemplate.execute(seckillReleaseScript, List.of(stockKey, userKey), String.valueOf(userId));
            throw ex;
        }
        return new SeckillResponse(requestId, remaining, "QUEUED");
    }

    /**
     * 通过 Feign 从商品活动服务读取活动信息。
     * <p>
     * 秒杀服务不直接访问活动表，避免跨服务直接读数据库。
     */
    private ActivityResponse loadActivity(Long activityId) {
        Result<ActivityResponse> result = activityClient.getActivity(activityId);
        if (!result.success() || result.data() == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "活动不存在");
        }
        return result.data();
    }

    /**
     * 校验秒杀活动是否合法。
     * <p>
     * 包括活动类型、启用状态、开始时间和结束时间。
     */
    private void validateActivity(ActivityResponse activity, boolean adminPreview) {
        if (!ActivityType.SECKILL.name().equals(activity.type())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "该活动不是秒杀活动");
        }
        if (activity.status() == null || activity.status() != 1) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "活动未启用");
        }
        LocalDateTime now = LocalDateTime.now();
        if (!adminPreview && now.isBefore(activity.startTime())) {
            throw new BizException(ErrorCode.ACTIVITY_NOT_STARTED,
                    "秒杀将于 " + activity.startTime() + " 开始");
        }
        if (now.isAfter(activity.endTime())) {
            throw new BizException(ErrorCode.ACTIVITY_ENDED);
        }
    }

    /**
     * 确保 Redis 中已经存在秒杀库存 key。
     * <p>
     * 正常情况下由商品服务预热库存；这里做兜底初始化，方便本地演示和调试。
     */
    private void ensureStockKey(ActivityResponse activity) {
        String stockKey = RedisKeys.SECKILL_STOCK + activity.id();
        Boolean created = redisTemplate.opsForValue().setIfAbsent(stockKey, String.valueOf(activity.availableStock()));
        if (Boolean.TRUE.equals(created)) {
            // 首次把库存灌入 Redis 时顺手清空参与用户集合，避免旧脏数据影响演示。
            redisTemplate.delete(RedisKeys.SECKILL_USER + activity.id());
        }
    }
}
