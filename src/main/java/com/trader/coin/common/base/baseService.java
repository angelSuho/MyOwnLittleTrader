package com.trader.coin.common.base;

import com.trader.coin.upbit.service.UpbitService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

//@Configuration
//@RequiredArgsConstructor
//public class baseService implements ApplicationRunner {
//    private final UpbitService upbitService;
//
//    @Override
//    public void run(ApplicationArguments args) {
//        upbitService.calculateProfitPercentage();
//        upbitService.waitAndSeeOrderCoin();
//    }
//}
