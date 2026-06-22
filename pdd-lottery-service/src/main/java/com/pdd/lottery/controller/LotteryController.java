package com.pdd.lottery.controller;

import com.pdd.common.api.ErrorCode;
import com.pdd.common.api.Result;
import com.pdd.common.exception.BizException;
import com.pdd.common.security.UserContext;
import com.pdd.lottery.dto.LotteryDrawResponse;
import com.pdd.lottery.dto.LotteryJoinResponse;
import com.pdd.lottery.dto.LotteryRequest;
import com.pdd.lottery.dto.LotterySpinResponse;
import com.pdd.lottery.dto.LotteryWinnerFeedItem;
import com.pdd.lottery.dto.PrizeSlice;
import com.pdd.lottery.service.LotteryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 抽奖接口。
 * <p>
 * 同时提供报名、轮盘抽奖、奖池查询、统一开奖和中奖查询。
 */
@RestController
@RequestMapping("/api/lottery")
public class LotteryController {
    private final LotteryService lotteryService;

    /**
     * 注入抽奖服务。
     */
    public LotteryController(LotteryService lotteryService) {
        this.lotteryService = lotteryService;
    }

    /**
     * 报名抽奖接口。
     */
    @PostMapping("/join")
    public Result<LotteryJoinResponse> join(@Valid @RequestBody LotteryRequest request) {
        Long userId = currentUserId();
        return Result.ok(lotteryService.join(userId, request.activityId(), UserContext.isAdmin()));
    }

    /**
     * 轮盘即时抽奖接口。
     */
    @PostMapping("/spin")
    public Result<LotterySpinResponse> spin(@Valid @RequestBody LotteryRequest request) {
        Long userId = currentUserId();
        return Result.ok(lotteryService.spin(userId, request.activityId(), UserContext.isAdmin()));
    }

    /**
     * 查询奖池配置接口。
     */
    @GetMapping("/prizes")
    public Result<List<PrizeSlice>> prizes(@RequestParam(value = "activityId", required = false) Long activityId) {
        return Result.ok(activityId == null ? lotteryService.prizePool() : lotteryService.prizePool(activityId));
    }

    /**
     * 手动触发统一开奖接口。
     */
    @PostMapping("/draw")
    public Result<LotteryDrawResponse> draw(@Valid @RequestBody LotteryRequest request) {
        return Result.ok(lotteryService.draw(request.activityId(), UserContext.isAdmin()));
    }

    /**
     * 查询当前用户是否中奖。
     */
    @GetMapping("/{activityId}/winner")
    public Result<Boolean> isWinner(@PathVariable("activityId") Long activityId) {
        Long userId = currentUserId();
        return Result.ok(lotteryService.isWinner(userId, activityId));
    }

    /**
     * 查询系统其他用户最近高等级中奖播报。
     */
    @GetMapping("/{activityId}/winner-feed")
    public Result<List<LotteryWinnerFeedItem>> winnerFeed(@PathVariable("activityId") Long activityId) {
        return Result.ok(lotteryService.winnerFeed(activityId, currentUserId()));
    }

    /**
     * 获取当前登录用户 ID，未登录时抛出统一未授权异常。
     */
    private Long currentUserId() {
        Long userId = UserContext.userId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}
