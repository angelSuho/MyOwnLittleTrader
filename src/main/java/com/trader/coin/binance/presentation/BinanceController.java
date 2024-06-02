package com.trader.coin.binance.presentation;

import com.trader.coin.binance.service.BinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coin/binance")
public class BinanceController {
    private final BinanceService binanceService;

    @GetMapping("/markets")
    public ResponseEntity<List<String>> getMarkets() {
        return ResponseEntity.status(HttpStatus.OK).body(binanceService.findMarkets());
    }
}
