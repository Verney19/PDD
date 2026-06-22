package com.pdd.order.controller;

import com.pdd.common.api.ErrorCode;
import com.pdd.common.api.Result;
import com.pdd.common.exception.BizException;
import com.pdd.common.security.UserContext;
import com.pdd.order.entity.Order;
import com.pdd.order.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 订单查询接口。
 * <p>
 * 当前只开放“查询当前用户订单”。
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    /**
     * 注入订单服务。
     */
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 查询当前登录用户的订单。
     */
    @GetMapping("/mine")
    public Result<List<Order>> listMine() {
        Long userId = UserContext.userId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return Result.ok(orderService.listUserOrders(userId));
    }
}
