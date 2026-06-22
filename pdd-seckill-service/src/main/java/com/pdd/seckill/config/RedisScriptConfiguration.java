package com.pdd.seckill.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * 秒杀 Lua 脚本配置。
 * <p>
 * 把扣库存脚本和回滚脚本注册为 Spring Bean，服务层可以直接注入执行。
 */
@Configuration
public class RedisScriptConfiguration {
    /**
     * 注册秒杀 Lua 脚本，负责原子扣库存和一人一单校验。
     */
    @Bean
    public DefaultRedisScript<Long> seckillScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/seckill.lua"));
        script.setResultType(Long.class);
        return script;
    }

    /**
     * 注册秒杀回滚 Lua 脚本，消息发送失败时用于恢复 Redis 状态。
     */
    @Bean
    public DefaultRedisScript<Long> seckillReleaseScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/seckill_release.lua"));
        script.setResultType(Long.class);
        return script;
    }
}
