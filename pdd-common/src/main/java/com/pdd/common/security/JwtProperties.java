package com.pdd.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置属性。
 */
@ConfigurationProperties(prefix = "pdd.jwt")
public class JwtProperties {
    private String secret = "pdd-flash-sale-platform-secret-key-must-be-32-bytes";
    private long ttlSeconds = 86400;

    /**
     * 获取 JWT 签名密钥。
     */
    public String getSecret() {
        return secret;
    }

    /**
     * 设置 JWT 签名密钥。
     */
    public void setSecret(String secret) {
        this.secret = secret;
    }

    /**
     * 获取 token 过期时间，单位秒。
     */
    public long getTtlSeconds() {
        return ttlSeconds;
    }

    /**
     * 设置 token 过期时间，单位秒。
     */
    public void setTtlSeconds(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }
}
