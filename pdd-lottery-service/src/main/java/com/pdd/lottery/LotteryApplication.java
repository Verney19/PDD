package com.pdd.lottery;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 抽奖服务启动入口。
 */
@EnableScheduling
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.pdd.common.client")
@MapperScan("com.pdd.lottery.mapper")
@SpringBootApplication(scanBasePackages = "com.pdd")
public class LotteryApplication {
    /**
     * 启动抽奖服务。
     */
    public static void main(String[] args) {
        SpringApplication.run(LotteryApplication.class, args);
    }
}
