package com.pdd.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 认证服务启动入口。
 */
@EnableDiscoveryClient
@MapperScan("com.pdd.auth.mapper")
@SpringBootApplication(scanBasePackages = "com.pdd")
public class AuthApplication {
    /**
     * 启动认证服务。
     */
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
