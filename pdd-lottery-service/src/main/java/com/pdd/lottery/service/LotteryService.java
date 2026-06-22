package com.pdd.lottery.service;

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
import com.pdd.lottery.dto.LotteryDrawResponse;
import com.pdd.lottery.dto.LotteryJoinResponse;
import com.pdd.lottery.dto.LotterySpinResponse;
import com.pdd.lottery.dto.LotteryWinnerFeedItem;
import com.pdd.lottery.dto.PrizeSlice;
import com.pdd.lottery.entity.LotteryPrize;
import com.pdd.lottery.mapper.LotteryPrizeMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * 抽奖服务核心业务。
 * <p>
 * 同时支持：
 * 1. 老的“报名 + 统一开奖”模式
 * 2. 当前前端轮盘使用的“用户即时抽奖”模式
 */
@Service
public class LotteryService {
    private static final Logger log = LoggerFactory.getLogger(LotteryService.class);
    private static final Long DEFAULT_PRIZE_ACTIVITY_ID = 100001L;
    private static final int USER_DAILY_SPIN_LIMIT = 3;
    private static final String DAILY_LIMIT_MESSAGE = "您今日的3次抽奖已用尽，请明天再来";
    private static final DefaultRedisScript<Long> DAILY_LIMIT_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('GET', KEYS[1])
            if current and tonumber(current) >= tonumber(ARGV[1]) then
                return -1
            end
            local next = redis.call('INCR', KEYS[1])
            if next == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[2])
            end
            return next
            """, Long.class);
    private final Random random = new Random();

    private final ActivityClient activityClient;
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final LotteryPrizeMapper lotteryPrizeMapper;
    private final ObjectMapper objectMapper;

    /**
     * 注入活动 Feign 客户端、Redis 和 RabbitMQ 模板。
     */
    public LotteryService(ActivityClient activityClient,
                          StringRedisTemplate redisTemplate,
                          RabbitTemplate rabbitTemplate,
                          LotteryPrizeMapper lotteryPrizeMapper,
                          ObjectMapper objectMapper) {
        this.activityClient = activityClient;
        this.redisTemplate = redisTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.lotteryPrizeMapper = lotteryPrizeMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 报名抽奖。
     * <p>
     * 老版统一开奖模式使用该方法，把用户写入 Redis Set。
     * Redis Set 自带去重能力，可防止同一用户重复报名。
     */
    public LotteryJoinResponse join(Long userId, Long activityId, boolean adminPreview) {
        ActivityResponse activity = loadActivity(activityId);
        validateJoinTime(activity, adminPreview);
        String usersKey = RedisKeys.LOTTERY_USERS + activityId;
        // Redis Set 天然去重，适合做报名集合。
        Long added = redisTemplate.opsForSet().add(usersKey, String.valueOf(userId));
        if (added == null || added == 0) {
            throw new BizException(ErrorCode.DUPLICATE_PARTICIPATION, "你已经报名抽奖");
        }
        Long size = redisTemplate.opsForSet().size(usersKey);
        return new LotteryJoinResponse(activityId, userId, size == null ? 0 : size, "JOINED");
    }

    /**
     * 返回当前轮盘奖池配置。
     * <p>
     * 前端根据该列表渲染轮盘扇区和右侧奖池说明。
     */
    public List<PrizeSlice> prizePool() {
        return prizePool(DEFAULT_PRIZE_ACTIVITY_ID);
    }

    public List<PrizeSlice> prizePool(Long activityId) {
        return loadPrizePool(activityId);
    }

    /**
     * 执行轮盘即时抽奖。
     * <p>
     * 该方法是当前前端抽奖页的核心接口：
     * 1. 校验活动时间与状态
     * 2. 加载数据库奖池配置
     * 3. 普通用户每天最多三次，管理端每次调用都按权重重新抽奖
     * 4. 中奖则写入中奖集合并投递订单消息
     */
    public LotterySpinResponse spin(Long userId, Long activityId, boolean adminPreview) {
        ActivityResponse activity = loadActivity(activityId);
        validateJoinTime(activity, adminPreview);

        List<PrizeSlice> prizePool = loadPrizePool(activityId);
        if (!adminPreview) {
            consumeDailySpinChance(userId);
        }

        PrizeSlice prize = drawPrize(activityId, prizePool);
        String requestId = null;
        if (Boolean.TRUE.equals(prize.winning())) {
            redisTemplate.opsForSet().add(RedisKeys.LOTTERY_SPIN_WINNERS + activityId, String.valueOf(userId));
            requestId = sendPrizeMessage(activity, activityId, userId, prize);
            recordWinnerFeed(activityId, userId, prize);
        }

        redisTemplate.opsForSet().add(RedisKeys.LOTTERY_USERS + activityId, String.valueOf(userId));
        return spinResponse(activityId, userId, prize, requestId, prizePool, "SPUN");
    }

    private void consumeDailySpinChance(Long userId) {
        String countKey = RedisKeys.LOTTERY_SPIN_DAILY_COUNT + LocalDate.now() + ":" + userId;
        Long count = redisTemplate.execute(
                DAILY_LIMIT_SCRIPT,
                List.of(countKey),
                String.valueOf(USER_DAILY_SPIN_LIMIT),
                String.valueOf(Duration.between(LocalDateTime.now(), LocalDate.now().plusDays(1).atStartOfDay()).toMillis())
        );
        if (count == null || count < 0) {
            throw new BizException(ErrorCode.TOO_MANY_REQUESTS, DAILY_LIMIT_MESSAGE);
        }
    }

    /**
     * 统一开奖。
     * <p>
     * 该方法用于“先报名、后开奖”的模式：
     * 通过 Redis 分布式锁保证同一活动只会被一个实例开奖，
     * 再从报名集合中随机抽取中奖用户并批量投递订单消息。
     */
    public LotteryDrawResponse draw(Long activityId, boolean adminPreview) {
        ActivityResponse activity = loadActivity(activityId);
        validateDrawTime(activity, adminPreview);

        String doneKey = RedisKeys.LOTTERY_DRAW_DONE + activityId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(doneKey))) {
            throw new BizException(ErrorCode.CONFLICT, "开奖已经完成");
        }

        String lockKey = RedisKeys.LOTTERY_DRAW_LOCK + activityId;
        // 统一开奖使用分布式锁控制，只允许一台实例执行开奖任务。
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofMinutes(30));
        if (!Boolean.TRUE.equals(locked)) {
            throw new BizException(ErrorCode.CONFLICT, "开奖任务正在执行或已经执行");
        }

        String usersKey = RedisKeys.LOTTERY_USERS + activityId;
        String winnersKey = RedisKeys.LOTTERY_WINNERS + activityId;
        Long participantCount = redisTemplate.opsForSet().size(usersKey);
        Set<String> existingWinners = redisTemplate.opsForSet().members(winnersKey);
        if (existingWinners != null && !existingWinners.isEmpty()) {
            sendWinnerMessages(activity, activityId, existingWinners);
            redisTemplate.opsForValue().set(doneKey, "1");
            return new LotteryDrawResponse(activityId, participantCount == null ? 0 : participantCount, existingWinners.size(), "QUEUED");
        }

        int winners = Math.min(activity.availableStock(), participantCount == null ? 0 : participantCount.intValue());
        if (winners <= 0) {
            redisTemplate.opsForValue().set(doneKey, "1");
            return new LotteryDrawResponse(activityId, participantCount == null ? 0 : participantCount, 0, "NO_WINNER");
        }

        // 直接从报名集合里随机抽取中奖人，适合演示场景。
        Set<String> winnerIds = redisTemplate.opsForSet().distinctRandomMembers(usersKey, winners);
        if (winnerIds == null || winnerIds.isEmpty()) {
            redisTemplate.opsForValue().set(doneKey, "1");
            return new LotteryDrawResponse(activityId, participantCount == null ? 0 : participantCount, 0, "NO_WINNER");
        }

        winnerIds.forEach(winnerId -> redisTemplate.opsForSet().add(winnersKey, winnerId));
        int queued = sendWinnerMessages(activity, activityId, winnerIds);
        redisTemplate.opsForValue().set(doneKey, "1");
        return new LotteryDrawResponse(activityId, participantCount == null ? 0 : participantCount, queued, "QUEUED");
    }

    /**
     * 给一批中奖用户发送订单消息。
     * <p>
     * 统一开奖模式下，中奖集合中的每个用户都会产生一条下单消息。
     */
    private int sendWinnerMessages(ActivityResponse activity, Long activityId, Set<String> winnerIds) {
        int queued = 0;
        for (String winnerId : winnerIds) {
            sendOrderMessage(activity, activityId, Long.valueOf(winnerId),
                    OrderSource.LOTTERY.name().toLowerCase() + ":" + activityId + ":" + winnerId,
                    activity.activityPrice());
            queued++;
        }
        return queued;
    }

    /**
     * 查询用户是否中奖。
     * <p>
     * 同时兼容统一开奖中奖集合和轮盘即时抽奖中奖集合。
     */
    public boolean isWinner(Long userId, Long activityId) {
        Boolean batchWinner = redisTemplate.opsForSet().isMember(RedisKeys.LOTTERY_WINNERS + activityId, String.valueOf(userId));
        Boolean spinWinner = redisTemplate.opsForSet().isMember(RedisKeys.LOTTERY_SPIN_WINNERS + activityId, String.valueOf(userId));
        return Boolean.TRUE.equals(batchWinner) || Boolean.TRUE.equals(spinWinner);
    }

    /**
     * 查询最近高等级中奖播报。
     */
    public List<LotteryWinnerFeedItem> winnerFeed(Long activityId, Long currentUserId) {
        String feedKey = RedisKeys.LOTTERY_WINNER_FEED + activityId;
        List<String> rows = redisTemplate.opsForList().range(feedKey, 0, 39);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream()
                .map(this::parseWinnerFeedItem)
                .filter(Objects::nonNull)
                .filter(item -> currentUserId == null || !currentUserId.equals(item.userId()))
                .limit(8)
                .toList();
    }

    /**
     * 定时开奖入口。
     * <p>
     * 定时任务调用该方法，扫描所有启用的抽奖活动，到开始时间后尝试开奖。
     */
    public void scheduledDraw() {
        Result<List<ActivityResponse>> result = activityClient.listActivities();
        if (!result.success() || result.data() == null) {
            log.warn("skip lottery schedule because activity service unavailable: {}", result.message());
            return;
        }
        result.data().stream()
                .filter(activity -> ActivityType.LOTTERY.name().equals(activity.type()))
                .filter(activity -> activity.status() != null && activity.status() == 1)
                .filter(activity -> !LocalDateTime.now().isBefore(activity.startTime()))
                .forEach(activity -> {
                    try {
                        // 定时任务走和手动开奖同一套逻辑，避免两套代码分叉。
                        draw(activity.id(), false);
                    } catch (BizException ex) {
                        log.info("lottery draw skipped activityId={} reason={}", activity.id(), ex.getMessage());
                    }
                });
    }

    /**
     * 加载并校验抽奖活动基础信息。
     * <p>
     * 抽奖服务只处理 LOTTERY 类型活动，其他类型直接拒绝。
     */
    private ActivityResponse loadActivity(Long activityId) {
        Result<ActivityResponse> result = activityClient.getActivity(activityId);
        if (!result.success() || result.data() == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "活动不存在");
        }
        ActivityResponse activity = result.data();
        if (!ActivityType.LOTTERY.name().equals(activity.type())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "该活动不是抽奖活动");
        }
        if (activity.status() == null || activity.status() != 1) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "活动未启用");
        }
        return activity;
    }

    /**
     * 校验用户是否可以报名或即时抽奖。
     * <p>
     * 当前规则是活动开始后、结束前允许参与。
     */
    private void validateJoinTime(ActivityResponse activity, boolean adminPreview) {
        if (adminPreview) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.startTime())) {
            throw new BizException(ErrorCode.ACTIVITY_NOT_STARTED,
                    "抽奖将于 " + activity.startTime() + " 开始");
        }
        if (now.isAfter(activity.endTime())) {
            throw new BizException(ErrorCode.ACTIVITY_ENDED, "抽奖已经结束");
        }
    }

    /**
     * 校验统一开奖是否可以执行。
     * <p>
     * 统一开奖只要求活动已经开始，重复开奖由 Redis doneKey/lockKey 控制。
     */
    private void validateDrawTime(ActivityResponse activity, boolean adminPreview) {
        if (adminPreview) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.startTime())) {
            throw new BizException(ErrorCode.ACTIVITY_NOT_STARTED,
                    "抽奖将于 " + activity.startTime() + " 开始");
        }
    }

    /**
     * 按奖池权重抽取一个奖项，并检查该奖项库存是否还有剩余。
     * <p>
     * 一、二、三等奖都有库存上限，通过 Redis 计数器控制；
     * 如果抽中的奖品库存已满，会继续尝试，最终兜底为参与奖。
     */
    private PrizeSlice drawPrize(Long activityId, List<PrizeSlice> prizePool) {
        for (int i = 0; i < 3; i++) {
            PrizeSlice candidate = weightedPrize(prizePool);
            if (candidate.stock() == null) {
                return candidate;
            }
            // 中奖奖项需要额外做库存计数，超过上限后继续尝试，最终兜底参与奖。
            Long count = redisTemplate.opsForValue().increment(RedisKeys.LOTTERY_PRIZE_COUNTER + activityId + ":" + candidate.code());
            if (count != null && count <= candidate.stock()) {
                return candidate;
            }
        }
        return prizePool.stream()
                .filter(prize -> !Boolean.TRUE.equals(prize.winning()))
                .findFirst()
                .orElse(prizePool.get(prizePool.size() - 1));
    }

    /**
     * 根据权重随机命中一个奖项。
     * <p>
     * 当前奖池权重总和通常为 10000，具体概率以数据库 pdd_lottery_prize.weight 为准。
     */
    private PrizeSlice weightedPrize(List<PrizeSlice> prizePool) {
        // 按配置好的权重区间随机命中一个奖项。
        int totalWeight = prizePool.stream().mapToInt(prize -> prize.weight() == null ? 0 : prize.weight()).sum();
        if (totalWeight <= 0) {
            return prizePool.get(prizePool.size() - 1);
        }
        int hit = random.nextInt(totalWeight) + 1;
        int cursor = 0;
        for (PrizeSlice prize : prizePool) {
            cursor += prize.weight() == null ? 0 : prize.weight();
            if (hit <= cursor) {
                return prize;
            }
        }
        return prizePool.get(prizePool.size() - 1);
    }

    private void recordWinnerFeed(Long activityId, Long userId, PrizeSlice prize) {
        if (!isHighValuePrize(prize)) {
            return;
        }
        LotteryWinnerFeedItem item = new LotteryWinnerFeedItem(
                activityId,
                userId,
                maskUserId(userId),
                prize.name(),
                prize.level(),
                LocalDateTime.now()
        );
        try {
            String feedKey = RedisKeys.LOTTERY_WINNER_FEED + activityId;
            redisTemplate.opsForList().leftPush(feedKey, objectMapper.writeValueAsString(item));
            redisTemplate.opsForList().trim(feedKey, 0, 49);
            redisTemplate.expire(feedKey, Duration.ofDays(7));
        } catch (JsonProcessingException ex) {
            log.warn("record lottery winner feed failed activityId={} userId={}", activityId, userId, ex);
        }
    }

    private LotteryWinnerFeedItem parseWinnerFeedItem(String value) {
        try {
            return objectMapper.readValue(value, LotteryWinnerFeedItem.class);
        } catch (JsonProcessingException ex) {
            log.warn("parse lottery winner feed failed", ex);
            return null;
        }
    }

    private boolean isHighValuePrize(PrizeSlice prize) {
        return prize != null
                && Boolean.TRUE.equals(prize.winning())
                && prize.level() != null
                && prize.level().matches(".*(超级大奖|特等奖|大奖|一等奖|二等奖|三等奖).*");
    }

    private String maskUserId(Long userId) {
        String text = String.valueOf(userId == null ? 0 : userId);
        if (text.length() <= 4) {
            return "用户 ****" + text;
        }
        return "用户 " + text.substring(0, 2) + "****" + text.substring(text.length() - 2);
    }

    /**
     * 根据奖品编码查找奖品定义。
     */
    private PrizeSlice prizeByCode(List<PrizeSlice> prizePool, String code) {
        return prizePool.stream()
                .filter(prize -> prize.code().equals(code))
                .findFirst()
                .orElse(prizePool.get(prizePool.size() - 1));
    }

    /**
     * 组装轮盘抽奖响应。
     * <p>
     * prizeIndex 用于前端控制轮盘最终停在哪个扇区。
     */
    private LotterySpinResponse spinResponse(Long activityId,
                                             Long userId,
                                             PrizeSlice prize,
                                             String requestId,
                                             List<PrizeSlice> prizePool,
                                             String status) {
        return new LotterySpinResponse(
                activityId,
                userId,
                prize,
                Math.max(0, prizePool.indexOf(prize)),
                prize.winning(),
                requestId,
                prizePool,
                status
        );
    }

    private List<PrizeSlice> loadPrizePool(Long activityId) {
        List<PrizeSlice> prizePool = lotteryPrizeMapper.selectList(Wrappers.<LotteryPrize>lambdaQuery()
                        .eq(LotteryPrize::getActivityId, activityId)
                        .orderByAsc(LotteryPrize::getSortOrder))
                .stream()
                .map(prize -> new PrizeSlice(
                        prize.getCode(),
                        prize.getName(),
                        prize.getLevel(),
                        prize.getWeight(),
                        prize.getStock(),
                        Boolean.TRUE.equals(prize.getWinning())
                ))
                .toList();
        if (prizePool.isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "奖池未配置");
        }
        return prizePool;
    }

    /**
     * 发送轮盘中奖订单消息。
     * <p>
     * requestId 保持最后一段为奖品编码，订单列表可以从 requestId 中反推出奖品名称。
     */
    private String sendPrizeMessage(ActivityResponse activity, Long activityId, Long userId, PrizeSlice prize) {
        // 每次抽奖生成独立请求号，避免同一用户多次中奖时被订单幂等逻辑吞掉。
        String drawId = UUID.randomUUID().toString().replace("-", "");
        String requestId = OrderSource.LOTTERY.name().toLowerCase() + ":spin:" + activityId + ":" + userId + ":" + drawId + ":" + prize.code();
        sendOrderMessage(activity, activityId, userId, requestId, BigDecimal.ZERO);
        return requestId;
    }

    /**
     * 向 RabbitMQ 投递订单创建消息。
     * <p>
     * 抽奖服务不直接写订单表，统一交给订单服务异步消费并落库。
     */
    private void sendOrderMessage(ActivityResponse activity, Long activityId, Long userId, String requestId, BigDecimal amount) {
        OrderCreateMessage message = new OrderCreateMessage(
                requestId,
                userId,
                activity.id(),
                activity.productId(),
                1,
                amount,
                OrderSource.LOTTERY.name(),
                LocalDateTime.now()
        );
        rabbitTemplate.convertAndSend(MqConstants.ORDER_EXCHANGE, MqConstants.LOTTERY_ORDER_ROUTING_KEY, message);
    }
}
