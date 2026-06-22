package com.pdd.lottery.schedule;

import com.pdd.lottery.service.LotteryService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 抽奖定时任务。
 * <p>
 * 在预设 cron 时间触发统一开奖，复用 LotteryService 中的开奖逻辑。
 */
@Component
public class LotteryScheduler {
    private final LotteryService lotteryService;

    /**
     * 注入抽奖服务，定时任务只负责编排，不重复实现开奖逻辑。
     */
    public LotteryScheduler(LotteryService lotteryService) {
        this.lotteryService = lotteryService;
    }

    /**
     * 到达预设时间后统一触发开奖。
     */
    @Scheduled(cron = "${pdd.lottery.draw-cron:0 45 20 18 6 ?}", zone = "Asia/Shanghai")
    public void drawOnSchedule() {
        lotteryService.scheduledDraw();
    }
}
