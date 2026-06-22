package com.pdd.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pdd.product.entity.Activity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 活动表 Mapper。
 */
public interface ActivityMapper extends BaseMapper<Activity> {
    /**
     * 按活动维度扣减数据库库存，返回受影响行数。
     */
    @Update("""
            update pdd_activity
            set available_stock = available_stock - #{quantity}, updated_at = now()
            where id = #{activityId}
              and available_stock >= #{quantity}
            """)
    int deductStock(@Param("activityId") Long activityId, @Param("quantity") int quantity);

    /**
     * 按活动维度回补数据库库存，返回受影响行数。
     */
    @Update("""
            update pdd_activity
            set available_stock = available_stock + #{quantity}, updated_at = now()
            where id = #{activityId}
            """)
    int releaseStock(@Param("activityId") Long activityId, @Param("quantity") int quantity);
}
