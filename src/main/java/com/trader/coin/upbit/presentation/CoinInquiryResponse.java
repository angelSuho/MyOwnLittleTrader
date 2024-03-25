package com.trader.coin.upbit.presentation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoinInquiryResponse {
    private String currency;
    private double balance;
    private double locked;
    @JsonProperty("avg_buy_price")
    private double avgBuyPrice;
    @JsonProperty("avg_buy_price_modified")
    private boolean avgBuyPriceModified;
    @JsonProperty("unit_currency")
    private String unitCurrency;

    public CoinInquiryResponse(String currency, double balance, double locked, double avgBuyPrice, boolean avgBuyPriceModified, String unitCurrency) {
        this.currency = currency;
        this.balance = balance;
        this.locked = locked;
        this.avgBuyPrice = avgBuyPrice;
        this.avgBuyPriceModified = avgBuyPriceModified;
        this.unitCurrency = unitCurrency;
    }
}
