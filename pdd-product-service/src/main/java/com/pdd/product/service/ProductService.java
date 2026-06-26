package com.pdd.product.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pdd.common.api.ErrorCode;
import com.pdd.common.exception.BizException;
import com.pdd.common.model.ActivityType;
import com.pdd.common.redis.RedisKeys;
import com.pdd.common.security.UserContext;
import com.pdd.product.dto.ActivityResponse;
import com.pdd.product.dto.CreateProductRequest;
import com.pdd.product.dto.ProductResponse;
import com.pdd.product.dto.UpdateProductRequest;
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
 * 负责商品 CRUD、活动查询、库存预热，以及订单服务回调时的数据库库存扣减/回补。
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
     * 查询商品列表，可按分类过滤。
     */
    public List<ProductResponse> listProducts(String category) {
        var query = Wrappers.lambdaQuery(Product.class);
        if (category != null && !category.isBlank()) {
            query.eq(Product::getCategory, category);
        }
        query.eq(Product::getStatus, 1).orderByAsc(Product::getId);
        return productMapper.selectList(query).stream()
                .map(this::toProductResponse)
                .toList();
    }

    /**
     * 根据 ID 查询单个商品。
     */
    public ProductResponse getProduct(Long id) {
        Product p = productMapper.selectById(id);
        if (p == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "商品不存在");
        }
        return toProductResponse(p);
    }

    /**
     * 创建商品（仅管理员）。
     */
    @Transactional(rollbackFor = Exception.class)
    public ProductResponse createProduct(CreateProductRequest req) {
        if (!UserContext.isAdmin()) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        Product p = new Product();
        p.setName(req.name());
        p.setPrice(req.price());
        p.setTotalStock(req.totalStock());
        p.setAvailableStock(req.availableStock());
        p.setCategory(req.category());
        p.setDescription(req.description());
        p.setImageUrl(req.imageUrl());
        p.setStatus(1);
        productMapper.insert(p);
        return toProductResponse(p);
    }

    /**
     * 更新商品（仅管理员）。
     */
    @Transactional(rollbackFor = Exception.class)
    public ProductResponse updateProduct(Long id, UpdateProductRequest req) {
        if (!UserContext.isAdmin()) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        Product p = productMapper.selectById(id);
        if (p == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "商品不存在");
        }
        p.setName(req.name());
        p.setPrice(req.price());
        p.setTotalStock(req.totalStock());
        p.setAvailableStock(req.availableStock());
        p.setCategory(req.category());
        p.setDescription(req.description());
        p.setImageUrl(req.imageUrl());
        p.setStatus(req.status());
        productMapper.updateById(p);
        return toProductResponse(p);
    }

    /**
     * 删除商品（仅管理员，且不能删除有关联活动的商品）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id) {
        if (!UserContext.isAdmin()) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        long activityCount = activityMapper.selectCount(
                Wrappers.lambdaQuery(Activity.class).eq(Activity::getProductId, id));
        if (activityCount > 0) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "该商品有关联活动，无法删除");
        }
        int rows = productMapper.deleteById(id);
        if (rows == 0) {
            throw new BizException(ErrorCode.NOT_FOUND, "商品不存在");
        }
    }

    /**
     * 查询全部活动列表，并拼接商品名称等展示信息。
     */
    public List<ActivityResponse> listActivities() {
        List<Activity> activities = activityMapper.selectList(Wrappers.lambdaQuery(Activity.class).orderByAsc(Activity::getStartTime));
        if (activities.isEmpty()) {
            return List.of();
        }
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
     */
    @Transactional(rollbackFor = Exception.class)
    public void deductStock(Long activityId, int quantity) {
        int rows = activityMapper.deductStock(activityId, quantity);
        if (rows == 0) {
            throw new BizException(ErrorCode.STOCK_NOT_ENOUGH);
        }
    }

    /**
     * 回补数据库中的活动库存。
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
     * 将商品实体转为响应对象。
     */
    private ProductResponse toProductResponse(Product p) {
        return new ProductResponse(
                p.getId(), p.getName(), p.getPrice(),
                p.getTotalStock(), p.getAvailableStock(),
                p.getCategory(), p.getDescription(), p.getImageUrl(), p.getStatus()
        );
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
