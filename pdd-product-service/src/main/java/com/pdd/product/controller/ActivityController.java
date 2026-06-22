package com.pdd.product.controller;

import com.pdd.common.api.Result;
import com.pdd.product.dto.ActivityResponse;
import com.pdd.product.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 面向前端暴露的活动接口。
 */
@RestController
@RequestMapping("/api/activities")
public class ActivityController {
    private final ProductService productService;

    /**
     * 注入商品活动服务。
     */
    public ActivityController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * 查询活动列表。
     */
    @GetMapping
    public Result<List<ActivityResponse>> listActivities() {
        return Result.ok(productService.listActivities());
    }

    /**
     * 根据活动 ID 查询活动详情。
     */
    @GetMapping("/{id}")
    public Result<ActivityResponse> getActivity(@PathVariable("id") Long id) {
        return Result.ok(productService.getActivity(id));
    }

    /**
     * 预热指定活动库存到 Redis。
     */
    @PostMapping("/{id}/preload")
    public Result<Void> preloadActivityStock(@PathVariable("id") Long id) {
        productService.preloadActivityStock(id);
        return Result.ok();
    }

    /**
     * 预热全部活动库存到 Redis。
     */
    @PostMapping("/preload")
    public Result<Void> preloadAllActivityStock() {
        productService.preloadAllActivityStock();
        return Result.ok();
    }
}
