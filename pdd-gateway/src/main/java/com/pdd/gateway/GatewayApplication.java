package com.pdd.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 网关服务启动入口。
 */
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.pdd")
public class GatewayApplication {
    /**
     * 启动网关服务。
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
