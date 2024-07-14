package com.trader.coin.common.presentation;

import com.trader.coin.binance.service.BinanceService;
import com.trader.coin.common.Tech.service.TechnicalIndicator;
import com.trader.coin.common.service.dto.CandleData;
import com.trader.coin.upbit.service.UpbitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommonController {
    private final BinanceService binanceService;
    private final UpbitService upbitService;

    private final TechnicalIndicator technicalIndicator;

    @GetMapping("ema")
    public ResponseEntity<List<Double>> getEMA(@RequestParam("market") String market) {
        List<CandleData> candles = binanceService.getCandles(market);
        Double ema15 = technicalIndicator.calculateEMA(candles, 15);
        Double ema50 = technicalIndicator.calculateEMA(candles, 50);
        return ResponseEntity.ok().body(List.of(ema15, ema50));
    }
}
