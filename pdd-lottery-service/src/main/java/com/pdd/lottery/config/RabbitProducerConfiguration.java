package com.pdd.lottery.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdd.common.mq.MqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 抽奖服务 RabbitMQ 生产端配置。
 * <p>
 * 抽中奖品后，由抽奖服务把中奖建单消息投递给订单服务异步落库。
 */
@Configuration
public class RabbitProducerConfiguration {
    /**
     * 配置抽奖服务发送 MQ 消息时使用的 JSON 转换器。
     */
    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * 配置抽奖服务的 RabbitTemplate。
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setMandatory(true);
        return template;
    }

    /**
     * 声明订单交换机。
     */
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(MqConstants.ORDER_EXCHANGE, true, false);
    }

    /**
     * 声明抽奖订单队列。
     */
    @Bean
    public Queue lotteryOrderQueue() {
        return new Queue(MqConstants.LOTTERY_ORDER_QUEUE, true, false, false, deadLetterArgs());
    }

    /**
     * 将抽奖订单队列绑定到订单交换机。
     */
    @Bean
    public Binding lotteryOrderBinding(@Qualifier("orderExchange") DirectExchange orderExchange,
                                       @Qualifier("lotteryOrderQueue") Queue lotteryOrderQueue) {
        return BindingBuilder.bind(lotteryOrderQueue).to(orderExchange).with(MqConstants.LOTTERY_ORDER_ROUTING_KEY);
    }

    /**
     * 抽奖队列死信参数。
     */
    private Map<String, Object> deadLetterArgs() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", MqConstants.ORDER_EXCHANGE);
        args.put("x-dead-letter-routing-key", MqConstants.ORDER_DEAD_ROUTING_KEY);
        return args;
    }
}
