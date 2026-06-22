package com.pdd.common.client;

import com.pdd.common.api.Result;
import com.pdd.common.client.dto.ActivityResponse;
import com.pdd.common.client.dto.StockRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 商品活动服务 Feign 客户端。
 * <p>
 * 秒杀、抽奖、订单服务都通过它访问活动信息和数据库库存接口。
 */
@FeignClient(name = "pdd-product-service", path = "/internal/activities")
public interface ActivityClient {
    /**
     * 查询全部活动列表，常用于定时任务或批量预热。
     */
    @GetMapping
    Result<List<ActivityResponse>> listActivities();

    /**
     * 按活动 ID 查询单个活动详情。
     */
    @GetMapping("/{id}")
    Result<ActivityResponse> getActivity(@PathVariable("id") Long id);

    /**
     * 扣减数据库活动库存。
     */
    @PostMapping("/deduct")
    Result<Void> deductStock(@RequestBody StockRequest request);

    /**
     * 回补数据库活动库存。
     */
    @PostMapping("/release")
    Result<Void> releaseStock(@RequestBody StockRequest request);
}
