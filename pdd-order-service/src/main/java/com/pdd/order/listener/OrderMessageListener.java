package com.pdd.order.listener;

import com.pdd.common.mq.MqConstants;
import com.pdd.common.mq.OrderCreateMessage;
import com.pdd.order.service.OrderService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 订单消息消费者。
 * <p>
 * 秒杀和抽奖最终都汇聚到订单服务消费，实现统一的建单逻辑。
 */
@Component
public class OrderMessageListener {
    private final OrderService orderService;

    /**
     * 注入订单服务。
     */
    public OrderMessageListener(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 消费秒杀订单消息。
     */
    @RabbitListener(queues = MqConstants.SECKILL_ORDER_QUEUE)
    public void handleSeckillOrder(OrderCreateMessage message) {
        orderService.createOrder(message);
    }

    /**
     * 消费抽奖订单消息。
     */
    @RabbitListener(queues = MqConstants.LOTTERY_ORDER_QUEUE)
    public void handleLotteryOrder(OrderCreateMessage message) {
        orderService.createOrder(message);
    }
}
