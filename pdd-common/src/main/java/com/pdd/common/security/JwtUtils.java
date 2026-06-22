package com.pdd.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 工具类。
 * <p>
 * 负责生成 token 和解析 token，网关和认证服务都会用到它。
 */
public class JwtUtils {
    private final JwtProperties properties;

    /**
     * 注入 JWT 配置。
     */
    public JwtUtils(JwtProperties properties) {
        this.properties = properties;
    }

    /**
     * 根据登录用户信息生成 JWT。
     */
    public String createToken(JwtUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                // subject 只存用户主键，其他信息放自定义 claim。
                .subject(String.valueOf(user.userId()))
                .claim("username", user.username())
                .claim("role", user.role())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(properties.getTtlSeconds())))
                .signWith(key())
                .compact();
    }

    /**
     * 校验并解析 JWT，返回项目内部统一使用的 JwtUser。
     */
    public JwtUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        // 解析后还原成项目内部统一使用的 JwtUser。
        return new JwtUser(
                Long.valueOf(claims.getSubject()),
                String.valueOf(claims.get("username")),
                String.valueOf(claims.get("role"))
        );
    }

    /**
     * 根据配置里的 secret 构造 HMAC 签名密钥。
     */
    private SecretKey key() {
        // JWT 使用 HMAC 对称签名，因此这里根据配置里的 secret 生成签名密钥。
        byte[] bytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(bytes);
    }
}
