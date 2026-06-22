package com.pdd.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体，对应 pdd_order 表。
 */
@TableName("pdd_order")
public class Order {
    @TableId(type = IdType.INPUT)
    private Long id;
    private String requestId;
    private Long userId;
    private Long activityId;
    private Long productId;
    private Integer quantity;
    private BigDecimal amount;
    private String source;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 获取订单 ID。
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置订单 ID。
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取请求幂等号。
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * 设置请求幂等号。
     */
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    /**
     * 获取下单用户 ID。
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置下单用户 ID。
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 获取活动 ID。
     */
    public Long getActivityId() {
        return activityId;
    }

    /**
     * 设置活动 ID。
     */
    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    /**
     * 获取商品 ID。
     */
    public Long getProductId() {
        return productId;
    }

    /**
     * 设置商品 ID。
     */
    public void setProductId(Long productId) {
        this.productId = productId;
    }

    /**
     * 获取下单数量。
     */
    public Integer getQuantity() {
        return quantity;
    }

    /**
     * 设置下单数量。
     */
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    /**
     * 获取订单金额。
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * 设置订单金额。
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * 获取订单来源。
     */
    public String getSource() {
        return source;
    }

    /**
     * 设置订单来源。
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * 获取订单状态。
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置订单状态。
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 获取创建时间。
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间。
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取更新时间。
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置更新时间。
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
