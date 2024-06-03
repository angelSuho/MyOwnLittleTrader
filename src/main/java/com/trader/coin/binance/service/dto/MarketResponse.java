package com.trader.coin.binance.service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MarketResponse {
    private String symbol;
    private String price;

    public MarketResponse(String symbol, String price) {
        this.symbol = symbol;
        this.price = price;
    }
}
