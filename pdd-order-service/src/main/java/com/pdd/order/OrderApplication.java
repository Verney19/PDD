package com.pdd.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 订单服务启动入口。
 */
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.pdd.common.client")
@MapperScan("com.pdd.order.mapper")
@SpringBootApplication(scanBasePackages = "com.pdd")
public class OrderApplication {
    /**
     * 启动订单服务。
     */
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
