package com.trader.coin.upbit.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class TickerResponse {
    private String market;
    @JsonProperty("trade_date")
    private String tradeDate;
    @JsonProperty("trade_time")
    private String tradeTime;
    @JsonProperty("trade_date_kst")
    private String tradeDateKst;
    @JsonProperty("trade_time_kst")
    private String tradeTimeKst;
    @JsonProperty("trade_timestamp")
    private long tradeTimestamp;
    @JsonProperty("opening_price")
    private double openingPrice;
    @JsonProperty("high_price")
    private double highPrice;
    @JsonProperty("low_price")
    private double lowPrice;
    @JsonProperty("trade_price")
    private double tradePrice;
    @JsonProperty("prev_closing_price")
    private double prevClosingPrice;
    private String change;
    @JsonProperty("change_price")
    private double changePrice;
    @JsonProperty("change_rate")
    private double changeRate;
    @JsonProperty("signed_change_price")
    private double signedChangePrice;
    @JsonProperty("signed_change_rate")
    private double signedChangeRate;
    @JsonProperty("trade_volume")
    private double tradeVolume;
    @JsonProperty("acc_trade_price")
    private double accTradePrice;
    @JsonProperty("acc_trade_price_24h")
    private double accTradePrice24h;
    @JsonProperty("acc_trade_volume")
    private double accTradeVolume;
    @JsonProperty("acc_trade_volume_24h")
    private double accTradeVolume24h;
    @JsonProperty("highest_52_week_price")
    private double highest52WeekPrice;
    @JsonProperty("highest_52_week_date")
    private String highest52WeekDate;
    @JsonProperty("lowest_52_week_price")
    private double lowest52WeekPrice;
    @JsonProperty("lowest_52_week_date")
    private String lowest52WeekDate;
    @JsonProperty("timestamp")
    private long timestamp;
}
