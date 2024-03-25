package com.trader.coin.upbit.presentation;

import com.trader.coin.upbit.service.UpbitService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coin")
public class UpbitController {
    private final UpbitService upbitService;

    @GetMapping("/markets")
    public ResponseEntity<List<String>> getMarkets() {
        return ResponseEntity.status(HttpStatus.OK).body(upbitService.findMarkets());
    }

    @GetMapping("/candles/minutes/{unit}")
    public ResponseEntity<List<CandleResponse>> getCandles(@PathVariable(value = "unit") int unit,
                                                           @NotNull @RequestParam(value = "market") String market,
                                                           @NotNull @RequestParam(value = "count") int count) {
        return ResponseEntity.status(HttpStatus.OK).body(upbitService.getCandles(unit, market, count));
    }

    @GetMapping("/my-inquiry")
    public ResponseEntity<List<CoinInquiryResponse>> getAccountInquiry() {
        return ResponseEntity.status(HttpStatus.OK).body(upbitService.getAccountInquiry());
    }

    @GetMapping("/wait-see-order")
    public ResponseEntity<Void> getWaitSeeOrder() {
        upbitService.waitAndSeeOrderCoin();
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/order")
    public ResponseEntity<Void> orderCoin(@Valid @RequestBody CoinOrderRequest coinOrderRequest) {
        upbitService.orderCoin(coinOrderRequest);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
