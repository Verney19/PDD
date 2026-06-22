package com.pdd.seckill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 秒杀服务启动入口。
 */
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.pdd.common.client")
@SpringBootApplication(scanBasePackages = "com.pdd")
public class SeckillApplication {
    /**
     * 启动秒杀服务。
     */
    public static void main(String[] args) {
        SpringApplication.run(SeckillApplication.class, args);
    }
}
