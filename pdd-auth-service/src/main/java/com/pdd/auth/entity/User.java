package com.pdd.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 用户实体，对应 pdd_user 表。
 */
@TableName("pdd_user")
public class User {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String username;
    private String password;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 获取用户主键。
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置用户主键。
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取用户名。
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置用户名。
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取加密后的密码。
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置加密后的密码。
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 获取用户角色。
     */
    public String getRole() {
        return role;
    }

    /**
     * 设置用户角色。
     */
    public void setRole(String role) {
        this.role = role;
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
