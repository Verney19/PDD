package com.pdd.order.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdd.common.mq.MqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.HashMap;
import java.util.Map;

/**
 * 订单服务 RabbitMQ 配置。
 * <p>
 * 订单服务既要消费秒杀/抽奖消息，也要统一声明队列、交换机和死信规则。
 */
@Configuration
public class RabbitConfiguration {

    /**
     * 配置 MQ 消息 JSON 转换器。
     */
    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * 配置 RabbitTemplate。
     * <p>
     * mandatory=true 可以在消息无法路由时触发 return 机制，便于排查投递问题。
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setMandatory(true);
        return template;
    }

    /**
     * 配置订单消息消费者容器。
     * <p>
     * 秒杀和抽奖都可能瞬间产生大量订单消息，因此这里设置并发消费者和预取数量。
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        // 适当提高并发消费者数量，适合秒杀/抽奖批量异步建单场景。
        factory.setPrefetchCount(50);
        factory.setConcurrentConsumers(4);
        factory.setMaxConcurrentConsumers(16);
        return factory;
    }

    /**
     * 订单主交换机。
     */
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(MqConstants.ORDER_EXCHANGE, true, false);
    }

    /**
     * 延迟交换机预留。
     */
    @Bean
    public DirectExchange orderDelayExchange() {
        return new DirectExchange(MqConstants.ORDER_DELAY_EXCHANGE, true, false);
    }

    /**
     * 秒杀订单队列。
     */
    @Bean
    public Queue seckillOrderQueue() {
        return new Queue(MqConstants.SECKILL_ORDER_QUEUE, true, false, false, deadLetterArgs());
    }

    /**
     * 抽奖订单队列。
     */
    @Bean
    public Queue lotteryOrderQueue() {
        return new Queue(MqConstants.LOTTERY_ORDER_QUEUE, true, false, false, deadLetterArgs());
    }

    /**
     * 订单死信队列。
     */
    @Bean
    public Queue orderDeadLetterQueue() {
        return new Queue(MqConstants.ORDER_DEAD_LETTER_QUEUE, true);
    }

    @Bean
    public Binding seckillOrderBinding(@Qualifier("orderExchange") DirectExchange orderExchange,
                                       @Qualifier("seckillOrderQueue") Queue seckillOrderQueue) {
        return BindingBuilder.bind(seckillOrderQueue).to(orderExchange).with(MqConstants.SECKILL_ORDER_ROUTING_KEY);
    }

    /**
     * 把抽奖订单队列绑定到订单交换机。
     */
    @Bean
    public Binding lotteryOrderBinding(@Qualifier("orderExchange") DirectExchange orderExchange,
                                       @Qualifier("lotteryOrderQueue") Queue lotteryOrderQueue) {
        return BindingBuilder.bind(lotteryOrderQueue).to(orderExchange).with(MqConstants.LOTTERY_ORDER_ROUTING_KEY);
    }

    /**
     * 把死信队列绑定到订单交换机。
     */
    @Bean
    public Binding deadLetterBinding(@Qualifier("orderExchange") DirectExchange orderExchange,
                                     @Qualifier("orderDeadLetterQueue") Queue orderDeadLetterQueue) {
        return BindingBuilder.bind(orderDeadLetterQueue).to(orderExchange).with(MqConstants.ORDER_DEAD_ROUTING_KEY);
    }

    /**
     * 创建业务队列的死信参数。
     */
    private Map<String, Object> deadLetterArgs() {
        Map<String, Object> args = new HashMap<>();
        // 消费失败或被拒绝的消息统一进入死信队列，便于后续排查。
        args.put("x-dead-letter-exchange", MqConstants.ORDER_EXCHANGE);
        args.put("x-dead-letter-routing-key", MqConstants.ORDER_DEAD_ROUTING_KEY);
        return args;
    }
}
