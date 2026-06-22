package com.pdd.product.controller;

import com.pdd.common.api.Result;
import com.pdd.product.dto.ActivityResponse;
import com.pdd.product.dto.StockRequest;
import com.pdd.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 面向服务间调用的内部活动接口。
 * <p>
 * 秒杀、抽奖、订单服务通过 Feign 调它来查询活动和扣减数据库库存。
 */
@RestController
@RequestMapping("/internal/activities")
public class InternalActivityController {
    private final ProductService productService;

    /**
     * 注入商品活动服务。
     */
    public InternalActivityController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * 服务间调用：查询活动列表。
     */
    @GetMapping
    public Result<List<ActivityResponse>> listActivities() {
        return Result.ok(productService.listActivities());
    }

    /**
     * 服务间调用：查询活动详情。
     */
    @GetMapping("/{id}")
    public Result<ActivityResponse> getActivity(@PathVariable("id") Long id) {
        return Result.ok(productService.getActivity(id));
    }

    /**
     * 服务间调用：扣减数据库活动库存。
     */
    @PostMapping("/deduct")
    public Result<Void> deductStock(@Valid @RequestBody StockRequest request) {
        productService.deductStock(request.activityId(), request.quantity());
        return Result.ok();
    }

    /**
     * 服务间调用：回补数据库活动库存。
     */
    @PostMapping("/release")
    public Result<Void> releaseStock(@Valid @RequestBody StockRequest request) {
        productService.releaseStock(request.activityId(), request.quantity());
        return Result.ok();
    }
}
