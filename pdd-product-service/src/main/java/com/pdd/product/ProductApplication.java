package com.pdd.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 商品与活动服务启动入口。
 */
@EnableDiscoveryClient
@MapperScan("com.pdd.product.mapper")
@SpringBootApplication(scanBasePackages = "com.pdd")
public class ProductApplication {
    /**
     * 启动商品与活动服务。
     */
    public static void main(String[] args) {
        SpringApplication.run(ProductApplication.class, args);
    }
}
