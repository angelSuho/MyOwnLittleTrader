package com.trader.coin.common.base;

import com.trader.coin.binance.service.BinanceService;
import com.trader.coin.common.domain.Market;
import com.trader.coin.common.infrastructure.config.APIProperties;
import com.trader.coin.upbit.service.UpbitService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class baseService implements ApplicationRunner {
    private final APIProperties api;

    private final BinanceService binanceService;
    private final UpbitService upbitService;

    @Override
    public void run(ApplicationArguments args) {
        if (api.getCrypto_market() == Market.BINANCE) {
            binanceService.waitAndSeeOrderCoin();
        } else {
            upbitService.calculateProfitPercentage();
            upbitService.evaluateHoldingsForSell();
            upbitService.waitAndSeeOrderCoin();
        }
    }
}
