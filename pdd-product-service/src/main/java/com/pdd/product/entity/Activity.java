package com.pdd.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 活动实体，对应 pdd_activity 表。
 * <p>
 * 秒杀和抽奖都统一抽象为活动，只是 type 不同。
 */
@TableName("pdd_activity")
public class Activity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long productId;
    private String type;
    private BigDecimal activityPrice;
    private Integer totalStock;
    private Integer availableStock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 获取活动主键。
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置活动主键。
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取关联商品 ID。
     */
    public Long getProductId() {
        return productId;
    }

    /**
     * 设置关联商品 ID。
     */
    public void setProductId(Long productId) {
        this.productId = productId;
    }

    /**
     * 获取活动类型。
     */
    public String getType() {
        return type;
    }

    /**
     * 设置活动类型。
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * 获取活动价或活动金额。
     */
    public BigDecimal getActivityPrice() {
        return activityPrice;
    }

    /**
     * 设置活动价或活动金额。
     */
    public void setActivityPrice(BigDecimal activityPrice) {
        this.activityPrice = activityPrice;
    }

    /**
     * 获取活动总库存。
     */
    public Integer getTotalStock() {
        return totalStock;
    }

    /**
     * 设置活动总库存。
     */
    public void setTotalStock(Integer totalStock) {
        this.totalStock = totalStock;
    }

    /**
     * 获取活动剩余库存。
     */
    public Integer getAvailableStock() {
        return availableStock;
    }

    /**
     * 设置活动剩余库存。
     */
    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
    }

    /**
     * 获取活动开始时间。
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }

    /**
     * 设置活动开始时间。
     */
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    /**
     * 获取活动结束时间。
     */
    public LocalDateTime getEndTime() {
        return endTime;
    }

    /**
     * 设置活动结束时间。
     */
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    /**
     * 获取活动状态。
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * 设置活动状态。
     */
    public void setStatus(Integer status) {
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
