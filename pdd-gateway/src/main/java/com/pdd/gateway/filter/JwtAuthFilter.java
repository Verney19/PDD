package com.pdd.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdd.common.api.ErrorCode;
import com.pdd.common.api.Result;
import com.pdd.common.security.JwtUser;
import com.pdd.common.security.JwtUtils;
import com.pdd.common.security.UserContext;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 网关层统一鉴权过滤器。
 * <p>
 * 1. 白名单路径直接放行
 * 2. 非白名单路径校验 Authorization 中的 JWT
 * 3. 校验成功后把用户信息透传到下游微服务请求头
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {
    private static final String BEARER = "Bearer ";
    private final List<String> whiteList = List.of(
            "/",
            "/index.html",
            "/app.js",
            "/styles.css",
            "/favicon.ico",
            "/api/auth/**",
            "/actuator/**",
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**"
    );

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 注入 JWT 工具和 JSON 序列化工具。
     */
    public JwtAuthFilter(JwtUtils jwtUtils, ObjectMapper objectMapper) {
        this.jwtUtils = jwtUtils;
        this.objectMapper = objectMapper;
    }

    /**
     * 网关请求过滤入口。
     * <p>
     * 非白名单请求必须带 Bearer token，校验成功后将用户信息写入请求头。
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        // 登录、文档、静态资源等路径不需要鉴权。
        if (whiteList.stream().anyMatch(pattern -> pathMatcher.match(pattern, path))) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        // 非白名单请求必须携带 Bearer token。
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER)) {
            return unauthorized(exchange);
        }

        try {
            JwtUser user = jwtUtils.parseToken(authorization.substring(BEARER.length()));
            ServerHttpRequest request = exchange.getRequest().mutate()
                    // 网关只解析一次 token，下游服务直接从请求头读取用户上下文。
                    .header(UserContext.USER_ID_HEADER, String.valueOf(user.userId()))
                    .header(UserContext.USERNAME_HEADER, user.username())
                    .header(UserContext.ROLE_HEADER, user.role())
                    .build();
            return chain.filter(exchange.mutate().request(request).build());
        } catch (Exception ex) {
            return unauthorized(exchange);
        }
    }

    /**
     * 构造未登录响应。
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes;
        try {
            // 与项目统一返回体保持一致，方便前端统一处理 401。
            bytes = objectMapper.writeValueAsBytes(Result.fail(ErrorCode.UNAUTHORIZED.code(), ErrorCode.UNAUTHORIZED.message()));
        } catch (JsonProcessingException e) {
            bytes = "{\"code\":401,\"message\":\"unauthorized\"}".getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    /**
     * 返回过滤器执行顺序。
     */
    @Override
    public int getOrder() {
        // 尽量较早执行，避免未鉴权请求继续进入后续过滤链。
        return -100;
    }
}
