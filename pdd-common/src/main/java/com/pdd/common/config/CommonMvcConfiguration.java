package com.pdd.common.config;

import com.pdd.common.security.JwtProperties;
import com.pdd.common.security.JwtUtils;
import com.pdd.common.snowflake.SnowflakeIdGenerator;
import com.pdd.common.web.UserContextFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 公共 Spring MVC 配置。
 * <p>
 * 被各个 Servlet 型微服务复用，统一提供 JWT 工具、用户上下文过滤器和雪花 ID 生成器。
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class CommonMvcConfiguration {

    /**
     * 创建 JWT 工具类。
     */
    @Bean
    public JwtUtils jwtUtils(JwtProperties jwtProperties) {
        return new JwtUtils(jwtProperties);
    }

    /**
     * 创建用户上下文过滤器。
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public UserContextFilter userContextFilter() {
        // 只有 Servlet Web 应用才需要该过滤器，Gateway(WebFlux) 不走这里。
        return new UserContextFilter();
    }

    /**
     * 创建雪花 ID 生成器。
     */
    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator(@Value("${pdd.worker-id:1}") long workerId) {
        // 每个服务使用不同 workerId，避免分布式 ID 冲突。
        return new SnowflakeIdGenerator(workerId);
    }
}
