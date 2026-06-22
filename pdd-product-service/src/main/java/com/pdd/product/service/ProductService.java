package com.pdd.product.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pdd.common.api.ErrorCode;
import com.pdd.common.exception.BizException;
import com.pdd.common.model.ActivityType;
import com.pdd.common.redis.RedisKeys;
import com.pdd.product.dto.ActivityResponse;
import com.pdd.product.dto.ProductResponse;
import com.pdd.product.entity.Activity;
import com.pdd.product.entity.Product;
import com.pdd.product.mapper.ActivityMapper;
import com.pdd.product.mapper.ProductMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商品与活动服务核心业务。
 * <p>
 * 负责商品查询、活动查询、库存预热，以及订单服务回调时的数据库库存扣减/回补。
 */
@Service
public class ProductService {
    private final ProductMapper productMapper;
    private final ActivityMapper activityMapper;
    private final StringRedisTemplate redisTemplate;

    /**
     * 注入商品 Mapper、活动 Mapper 和 Redis 操作模板。
     */
    public ProductService(ProductMapper productMapper, ActivityMapper activityMapper, StringRedisTemplate redisTemplate) {
        this.productMapper = productMapper;
        this.activityMapper = activityMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 查询全部商品列表。
     */
    public List<ProductResponse> listProducts() {
        return productMapper.selectList(Wrappers.emptyWrapper()).stream()
                .map(p -> new ProductResponse(p.getId(), p.getName(), p.getPrice(), p.getTotalStock(), p.getAvailableStock()))
                .toList();
    }

    /**
     * 查询全部活动列表，并拼接商品名称等展示信息。
     */
    public List<ActivityResponse> listActivities() {
        List<Activity> activities = activityMapper.selectList(Wrappers.lambdaQuery(Activity.class).orderByAsc(Activity::getStartTime));
        if (activities.isEmpty()) {
            return List.of();
        }
        // 活动和商品是分表存储的，这里一次性批量查商品，避免循环查询。
        Map<Long, Product> productMap = productMapper.selectBatchIds(
                activities.stream().map(Activity::getProductId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        return activities.stream()
                .map(activity -> toResponse(activity, productMap.get(activity.getProductId())))
                .toList();
    }

    /**
     * 根据活动 ID 查询单个活动。
     */
    public ActivityResponse getActivity(Long id) {
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "活动不存在");
        }
        Product product = productMapper.selectById(activity.getProductId());
        return toResponse(activity, product);
    }

    /**
     * 扣减数据库中的活动库存。
     * <p>
     * 订单服务消费 MQ 后调用该方法完成最终库存落库。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deductStock(Long activityId, int quantity) {
        // 数据库层扣减是最终一致性的落库动作，前面高并发入口由 Redis 顶住。
        int rows = activityMapper.deductStock(activityId, quantity);
        if (rows == 0) {
            throw new BizException(ErrorCode.STOCK_NOT_ENOUGH);
        }
    }

    /**
     * 回补数据库中的活动库存。
     * <p>
     * 当订单落库失败时，订单服务会调用该方法回滚库存。
     */
    @Transactional(rollbackFor = Exception.class)
    public void releaseStock(Long activityId, int quantity) {
        activityMapper.releaseStock(activityId, quantity);
    }

    /**
     * 把某个活动的数据库库存预热到 Redis。
     */
    public void preloadActivityStock(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "活动不存在");
        }
        // 秒杀和抽奖分别预热到不同的 Redis key，便于服务侧按类型消费。
        if (ActivityType.SECKILL.name().equals(activity.getType())) {
            redisTemplate.opsForValue().set(RedisKeys.SECKILL_STOCK + activityId, String.valueOf(activity.getAvailableStock()));
        } else if (ActivityType.LOTTERY.name().equals(activity.getType())) {
            redisTemplate.opsForValue().set(RedisKeys.LOTTERY_STOCK + activityId, String.valueOf(activity.getAvailableStock()));
        }
    }

    /**
     * 预热全部活动库存。
     */
    public void preloadAllActivityStock() {
        activityMapper.selectList(Wrappers.emptyWrapper()).forEach(activity -> preloadActivityStock(activity.getId()));
    }

    /**
     * 将活动实体和商品实体组装成前端/服务间使用的活动响应对象。
     */
    private ActivityResponse toResponse(Activity activity, Product product) {
        return new ActivityResponse(
                activity.getId(),
                activity.getProductId(),
                product == null ? null : product.getName(),
                activity.getType(),
                activity.getActivityPrice(),
                activity.getTotalStock(),
                activity.getAvailableStock(),
                activity.getStartTime(),
                activity.getEndTime(),
                activity.getStatus()
        );
    }
}
