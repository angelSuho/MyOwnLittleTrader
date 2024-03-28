package com.trader.coin.common.base;

import com.trader.coin.upbit.infrastructure.ProfitPercentageRepository;
import com.trader.coin.upbit.service.UpbitService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class baseService {
    private final UpbitService upbitService;
    private final ProfitPercentageRepository profitPercentageRepository;

    @PostConstruct
    public void init() {
        upbitService.calculateProfitPercentage();
        upbitService.delayMethod(1000);
    }
}
