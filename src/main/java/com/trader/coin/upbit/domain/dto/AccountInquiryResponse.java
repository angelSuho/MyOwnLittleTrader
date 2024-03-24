package com.trader.coin.upbit.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountInquiryResponse {
    private String currency;
    private double balance;
    private int locked;
    @JsonProperty("avg_buy_price")
    private double avgBuyPrice;
    @JsonProperty("avg_buy_price_modified")
    private boolean avgBuyPriceModified;
    @JsonProperty("unit_currency")
    private String unitCurrency;

    public AccountInquiryResponse(String currency, double balance, int locked, double avgBuyPrice, boolean avgBuyPriceModified, String unitCurrency) {
        this.currency = currency;
        this.balance = balance;
        this.locked = locked;
        this.avgBuyPrice = avgBuyPrice;
        this.avgBuyPriceModified = avgBuyPriceModified;
        this.unitCurrency = unitCurrency;
    }
}
