package com.pdd.common.mq;

/**
 * RabbitMQ 交换机、队列、路由键常量。
 * <p>
 * 统一收口到这里，避免各服务手写字符串导致队列名不一致。
 */
public final class MqConstants {
    public static final String ORDER_EXCHANGE = "pdd.order.exchange";
    public static final String ORDER_DELAY_EXCHANGE = "pdd.order.delay.exchange";
    public static final String SECKILL_ORDER_QUEUE = "pdd.seckill.order.queue";
    public static final String LOTTERY_ORDER_QUEUE = "pdd.lottery.order.queue";
    public static final String ORDER_TIMEOUT_QUEUE = "pdd.order.timeout.queue";
    public static final String ORDER_DEAD_LETTER_QUEUE = "pdd.order.dead.queue";
    public static final String SECKILL_ORDER_ROUTING_KEY = "order.seckill.create";
    public static final String LOTTERY_ORDER_ROUTING_KEY = "order.lottery.create";
    public static final String ORDER_TIMEOUT_ROUTING_KEY = "order.timeout";
    public static final String ORDER_DEAD_ROUTING_KEY = "order.dead";

    /**
     * 工具类不允许实例化。
     */
    private MqConstants() {
    }
}
