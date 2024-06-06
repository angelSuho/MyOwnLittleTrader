package com.trader.coin.binance.presentation;

import com.trader.coin.binance.service.BinanceService;
import com.trader.coin.binance.service.dto.MarketResponse;
import com.trader.coin.binance.service.dto.TickerResponse;
import com.trader.coin.common.service.dto.CandleData;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coin/binance")
public class BinanceController {
    private final BinanceService binanceService;

    @GetMapping("/markets")
    public ResponseEntity<List<MarketResponse>> getMarkets() {
        return ResponseEntity.status(HttpStatus.OK).body(binanceService.getMarkets());
    }

    @GetMapping("/candles")
    public ResponseEntity<List<CandleData>> getCandles(@RequestParam("market") @NotNull String market) {
        return ResponseEntity.status(HttpStatus.OK).body(binanceService.getCandles(market));
    }

    @GetMapping("/ticker")
    public ResponseEntity<TickerResponse> getTicker(@RequestParam("market") @NotNull String market) {
        return ResponseEntity.status(HttpStatus.OK).body(binanceService.getTicker(market));
    }

    @GetMapping("/order")
    public ResponseEntity<Void> orderCoin() {
        binanceService.waitAndSeeOrderCoin();
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
