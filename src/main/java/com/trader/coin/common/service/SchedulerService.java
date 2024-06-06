package com.trader.coin.common.service;

import com.trader.coin.binance.service.BinanceService;
import com.trader.coin.common.domain.Market;
import com.trader.coin.common.infrastructure.config.APIProperties;
import com.trader.coin.upbit.service.UpbitService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SchedulerService {
    private final APIProperties api;

    private final BinanceService binanceService;
    private final UpbitService upbitService;

    @Scheduled(cron = "0 2 * * * *")
    public void taskAt2Minutes() {
        if (api.getCrypto_market() == Market.BINANCE) {
            binanceService.waitAndSeeOrderCoin();
        } else {
            upbitService.calculateProfitPercentage();
            upbitService.waitAndSeeOrderCoin();
        }
    }

    @Scheduled(cron = "0 12 * * * *")
    public void taskAt12Minutes() {
        if (api.getCrypto_market() == Market.UPBIT) {
            upbitService.calculateProfitPercentage();
        }
    }

    @Scheduled(cron = "0 22 * * * *")
    public void taskAt22Minutes() {
        if (api.getCrypto_market() == Market.UPBIT) {
            upbitService.calculateProfitPercentage();
        }
    }

    @Scheduled(cron = "0 32 * * * *")
    public void taskAt32Minutes() {
        if (api.getCrypto_market() == Market.UPBIT) {
            upbitService.calculateProfitPercentage();
            upbitService.waitAndSeeOrderCoin();
        }
    }

    @Scheduled(cron = "0 42 * * * *")
    public void taskAt42Minutes() {
        if (api.getCrypto_market() == Market.UPBIT) {
            upbitService.calculateProfitPercentage();
        }
    }

    @Scheduled(cron = "0 52 * * * *")
    public void taskAt52Minutes() {
        if (api.getCrypto_market() == Market.UPBIT) {
            upbitService.calculateProfitPercentage();
        }
    }

    @Scheduled(cron = "0 0/30 * * * *")
    public void 매도조건확인후매도() {
        if (api.getCrypto_market() == Market.UPBIT) {
            upbitService.evaluateHoldingsForSell();
        }
    }
}
