package com.pdd.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体，对应 pdd_product 表。
 */
@TableName("pdd_product")
public class Product {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer totalStock;
    private Integer availableStock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 获取商品主键。
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置商品主键。
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取商品名称。
     */
    public String getName() {
        return name;
    }

    /**
     * 设置商品名称。
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取商品原价。
     */
    public BigDecimal getPrice() {
        return price;
    }

    /**
     * 设置商品原价。
     */
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    /**
     * 获取商品总库存。
     */
    public Integer getTotalStock() {
        return totalStock;
    }

    /**
     * 设置商品总库存。
     */
    public void setTotalStock(Integer totalStock) {
        this.totalStock = totalStock;
    }

    /**
     * 获取商品可用库存。
     */
    public Integer getAvailableStock() {
        return availableStock;
    }

    /**
     * 设置商品可用库存。
     */
    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
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
