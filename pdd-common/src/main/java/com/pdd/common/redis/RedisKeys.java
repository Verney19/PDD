package com.pdd.common.redis;

/**
 * 项目统一 Redis Key 前缀常量。
 */
public final class RedisKeys {
    public static final String SECKILL_STOCK = "seckill:stock:";
    public static final String SECKILL_USER = "seckill:user:";
    public static final String SECKILL_REQUEST = "seckill:request:";
    public static final String LOTTERY_STOCK = "lottery:stock:";
    public static final String LOTTERY_USERS = "lottery:users:";
    public static final String LOTTERY_WINNERS = "lottery:winners:";
    public static final String LOTTERY_SPIN_DAILY_COUNT = "lottery:spin:daily:";
    public static final String LOTTERY_SPIN_WINNERS = "lottery:spin:winners:";
    public static final String LOTTERY_WINNER_FEED = "lottery:winner:feed:";
    public static final String LOTTERY_PRIZE_COUNTER = "lottery:prize:counter:";
    public static final String LOTTERY_REQUEST = "lottery:request:";
    public static final String LOTTERY_DRAW_LOCK = "lottery:draw:lock:";
    public static final String LOTTERY_DRAW_DONE = "lottery:draw:done:";

    /**
     * 工具类不允许实例化。
     */
    private RedisKeys() {
    }
}
