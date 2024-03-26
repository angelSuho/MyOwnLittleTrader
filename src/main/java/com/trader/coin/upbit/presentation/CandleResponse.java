package com.trader.coin.upbit.presentation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.*;

@Getter
@NoArgsConstructor(access = PROTECTED)
public class CandleResponse {
    private String market;
    @JsonProperty("candle_date_time_utc")
    private String candleDateTimeUtc;
    @JsonProperty("candle_date_time_kst")
    private String candleDateTimeKst;
    @JsonProperty("opening_price")
    private double openingPrice;
    @JsonProperty("high_price")
    private double highPrice;
    @JsonProperty("low_price")
    private double lowPrice;
    @JsonProperty("trade_price")
    private double tradePrice;
    private long timestamp;
    @JsonProperty("candle_acc_trade_price")
    private double candleAccTradePrice;
    @JsonProperty("candle_acc_trade_volume")
    private double candleAccTradeVolume;
    @JsonProperty("prev_closing_price")
    private Double prevClosingPrice;
    @JsonProperty("change_price")
    private Double changePrice;
    @JsonProperty("change_rate")
    private Double changeRate;
    private Integer unit;

    public CandleResponse(String market, String candleDateTimeUtc, String candleDateTimeKst,
                          double openingPrice, double highPrice, double lowPrice, double tradePrice,
                          long timestamp, double candleAccTradePrice, double candleAccTradeVolume,
                          int unit) {
        this.market = market;
        this.candleDateTimeUtc = candleDateTimeUtc;
        this.candleDateTimeKst = candleDateTimeKst;
        this.openingPrice = openingPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.tradePrice = tradePrice;
        this.timestamp = timestamp;
        this.candleAccTradePrice = candleAccTradePrice;
        this.candleAccTradeVolume = candleAccTradeVolume;
        this.unit = unit;
    }

    public CandleResponse(String market, String candleDateTimeUtc, String candleDateTimeKst,
                          double openingPrice, double highPrice, double lowPrice, double tradePrice,
                          long timestamp, double candleAccTradePrice, double candleAccTradeVolume,
                          Double prevClosingPrice, Double changePrice, Double changeRate) {
        this.market = market;
        this.candleDateTimeUtc = candleDateTimeUtc;
        this.candleDateTimeKst = candleDateTimeKst;
        this.openingPrice = openingPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.tradePrice = tradePrice;
        this.timestamp = timestamp;
        this.candleAccTradePrice = candleAccTradePrice;
        this.candleAccTradeVolume = candleAccTradeVolume;
        this.prevClosingPrice = prevClosingPrice;
        this.changePrice = changePrice;
        this.changeRate = changeRate;
    }
}
