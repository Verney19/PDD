package com.pdd.seckill.controller;

import com.pdd.common.api.ErrorCode;
import com.pdd.common.api.Result;
import com.pdd.common.exception.BizException;
import com.pdd.common.security.UserContext;
import com.pdd.seckill.dto.SeckillRequest;
import com.pdd.seckill.dto.SeckillResponse;
import com.pdd.seckill.service.SeckillService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 秒杀接口。
 */
@RestController
@RequestMapping("/api/seckill")
public class SeckillController {
    private final SeckillService seckillService;

    /**
     * 注入秒杀服务。
     */
    public SeckillController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    /**
     * 秒杀抢购接口。
     * <p>
     * 网关鉴权后会把用户信息写入请求头，这里通过 UserContext 读取当前 userId。
     */
    @PostMapping("/grab")
    public Result<SeckillResponse> grab(@Valid @RequestBody SeckillRequest request) {
        Long userId = UserContext.userId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return Result.ok(seckillService.seckill(userId, request.activityId(), UserContext.isAdmin()));
    }
}
