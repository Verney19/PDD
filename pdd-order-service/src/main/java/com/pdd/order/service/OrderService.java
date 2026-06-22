package com.pdd.order.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pdd.common.api.Result;
import com.pdd.common.client.ActivityClient;
import com.pdd.common.client.dto.StockRequest;
import com.pdd.common.model.OrderStatus;
import com.pdd.common.mq.OrderCreateMessage;
import com.pdd.common.snowflake.SnowflakeIdGenerator;
import com.pdd.order.entity.Order;
import com.pdd.order.mapper.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单服务核心业务。
 * <p>
 * 主要处理 MQ 下单消息、幂等建单和订单查询。
 */
@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderMapper orderMapper;
    private final ActivityClient activityClient;
    private final SnowflakeIdGenerator idGenerator;

    /**
     * 注入订单 Mapper、活动 Feign 客户端和 ID 生成器。
     */
    public OrderService(OrderMapper orderMapper, ActivityClient activityClient, SnowflakeIdGenerator idGenerator) {
        this.orderMapper = orderMapper;
        this.activityClient = activityClient;
        this.idGenerator = idGenerator;
    }

    /**
     * 根据 MQ 消息创建订单。
     * <p>
     * 该方法负责：
     * 1. 用 requestId 做消费幂等
     * 2. 调商品服务扣减数据库库存
     * 3. 生成订单主键并写入订单表
     * 4. 如果订单写入失败，调用商品服务回补库存
     */
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(OrderCreateMessage message) {
        // 先用 requestId 做幂等校验，避免 MQ 重投或重复消费造成重复订单。
        Long exists = orderMapper.selectCount(Wrappers.<Order>lambdaQuery().eq(Order::getRequestId, message.requestId()));
        if (exists > 0) {
            log.info("duplicate order message ignored requestId={}", message.requestId());
            return;
        }

        // Redis 预扣减只能保护高并发入口，最终库存仍需要落到数据库。
        Result<Void> stockResult = activityClient.deductStock(new StockRequest(message.activityId(), message.quantity()));
        if (!stockResult.success()) {
            throw new IllegalStateException("deduct stock failed: " + stockResult.message());
        }

        Order order = new Order();
        order.setId(idGenerator.nextId());
        order.setRequestId(message.requestId());
        order.setUserId(message.userId());
        order.setActivityId(message.activityId());
        order.setProductId(message.productId());
        order.setQuantity(message.quantity());
        order.setAmount(message.amount());
        order.setSource(message.source());
        order.setStatus(OrderStatus.CREATED.name());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        try {
            orderMapper.insert(order);
        } catch (RuntimeException ex) {
            // 数据库写订单失败时，把数据库库存补回去，避免库存永久丢失。
            activityClient.releaseStock(new StockRequest(message.activityId(), message.quantity()));
            throw ex;
        }
    }

    /**
     * 查询指定用户的订单列表，按创建时间倒序返回。
     */
    public List<Order> listUserOrders(Long userId) {
        return orderMapper.selectList(Wrappers.<Order>lambdaQuery()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreatedAt));
    }
}
