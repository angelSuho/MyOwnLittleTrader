package com.trader.coin.binance.service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TickerResponse {
    private String symbol;
    private String priceChange;
    private String priceChangePercent;
    private String weightedAvgPrice;
    private String lastPrice;
    private String lastQty;
    private String openPrice;
    private String highPrice;
    private String lowPrice;
    private String volume;
    private String quoteVolume;
    private String openTime;
    private String closeTime;
    private String firstId;
    private String lastId;
    private String count;

    public TickerResponse(String symbol, String priceChange, String priceChangePercent, String weightedAvgPrice, String lastPrice, String lastQty, String openPrice, String highPrice, String lowPrice, String volume,
                          String quoteVolume, String openTime, String closeTime, String firstId, String lastId, String count) {
        this.symbol = symbol;
        this.priceChange = priceChange;
        this.priceChangePercent = priceChangePercent;
        this.weightedAvgPrice = weightedAvgPrice;
        this.lastPrice = lastPrice;
        this.lastQty = lastQty;
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.volume = volume;
        this.quoteVolume = quoteVolume;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.firstId = firstId;
        this.lastId = lastId;
        this.count = count;
    }
}
