package com.trader.coin.common.service;

import com.trader.coin.upbit.service.UpbitService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SchedulerService {
    private final UpbitService upbitService;

    @Scheduled(cron = "0 2 * * * *")
    public void taskAt2Minutes() {
        upbitService.calculateProfitPercentage();
    }

    @Scheduled(cron = "0 12 * * * *")
    public void taskAt12Minutes() {
        upbitService.calculateProfitPercentage();
    }

    @Scheduled(cron = "0 22 * * * *")
    public void taskAt22Minutes() {
        upbitService.calculateProfitPercentage();
    }

    @Scheduled(cron = "0 32 * * * *")
    public void taskAt32Minutes() {
        upbitService.calculateProfitPercentage();
    }

    @Scheduled(cron = "0 42 * * * *")
    public void taskAt42Minutes() {
        upbitService.calculateProfitPercentage();
    }

    @Scheduled(cron = "0 52 * * * *")
    public void taskAt52Minutes() {
        upbitService.calculateProfitPercentage();
    }

    @Scheduled(cron = "0 0/30 * * * *")
    public void 매도조건확인후매도() {
        upbitService.evaluateHoldingsForSell();
    }

    @Scheduled(cron = "0 0 */1 * * *")
    public void 매수조건확인후매수() {
        upbitService.waitAndSeeOrderCoin();
    }
}
