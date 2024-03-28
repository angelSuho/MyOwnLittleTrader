package com.trader.coin.upbit.presentation;

import com.trader.coin.common.Tech.service.TechnicalIndicator;
import com.trader.coin.upbit.presentation.dto.CoinInquiryResponse;
import com.trader.coin.upbit.presentation.dto.CoinOrderRequest;
import com.trader.coin.upbit.service.dto.CandleResponse;
import com.trader.coin.upbit.service.UpbitService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coin")
public class UpbitController {
    private final UpbitService upbitService;
    private final TechnicalIndicator technicalIndicator;

    @GetMapping("/markets")
    public ResponseEntity<List<String>> getMarkets() {
        return ResponseEntity.status(HttpStatus.OK).body(upbitService.findMarkets());
    }

    @GetMapping("/candles/{unit}")
    public ResponseEntity<List<CandleResponse>> getCandles(@PathVariable(value = "unit") String unit,
                                                           @NotNull @RequestParam(value = "market") String market,
                                                           @NotNull @RequestParam(value = "count") int count) {
        List<CandleResponse> candles = upbitService.getCandles(unit, market, count);
        System.out.println(Arrays.toString(technicalIndicator.calculateBollingerBand(candles, 20)));
        return ResponseEntity.status(HttpStatus.OK).body(candles);
    }

    @GetMapping("/my-inquiry")
    public ResponseEntity<List<CoinInquiryResponse>> getAccountInquiry() {
        return ResponseEntity.status(HttpStatus.OK).body(upbitService.getAccountInquiry());
    }

    @GetMapping("/try-order")
    public ResponseEntity<Void> tryOrderCoin() {
        upbitService.waitAndSeeOrderCoin();
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/holding-sell")
    public ResponseEntity<Void> evaluateHoldingsForSell() {
        upbitService.evaluateHoldingsForSell();
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/calculate-profit")
    public ResponseEntity<Void> calculateProfit() {
        upbitService.calculateProfitPercentage();
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/order")
    public ResponseEntity<Void> orderCoin(@Valid @RequestBody CoinOrderRequest coinOrderRequest) {
        upbitService.orderCoin(coinOrderRequest);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
