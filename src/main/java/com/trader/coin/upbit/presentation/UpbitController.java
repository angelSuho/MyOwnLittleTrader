package com.trader.coin.upbit.presentation;

import com.trader.coin.upbit.domain.dto.AccountInquiryResponse;
import com.trader.coin.upbit.domain.dto.UpbitOrderRequest;
import com.trader.coin.upbit.service.UpbitService;
import jakarta.validation.Valid;
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

    @GetMapping("/my-inquiry")
    public ResponseEntity<List<AccountInquiryResponse>> getAccountInquiry() {
        return ResponseEntity.status(HttpStatus.OK).body(upbitService.getAccountInquiry());
    }

    @PostMapping("/order")
    public ResponseEntity<Void> orderCoin(@Valid @RequestBody UpbitOrderRequest upbitOrderRequest) {
        upbitService.orderCoin(upbitOrderRequest);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
