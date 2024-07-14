package com.trader.coin.upbit.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trader.coin.common.service.dto.CandleData;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Objects;

import static lombok.AccessLevel.*;

@Getter
@NoArgsConstructor(access = PROTECTED)
public class UpbitCandleResponse extends CandleData {
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

    public UpbitCandleResponse(String market, String candleDateTimeUtc, String candleDateTimeKst,
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

    public UpbitCandleResponse(String market, String candleDateTimeUtc, String candleDateTimeKst,
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

    public UpbitCandleResponse(LinkedHashMap<String, Object> linkedHashMap) {
        this.market = (String) linkedHashMap.get("market");
        this.candleDateTimeUtc = (String) linkedHashMap.get("candle_date_time_utc");
        this.candleDateTimeKst = (String) linkedHashMap.get("candle_date_time_kst");
        this.openingPrice = (double) linkedHashMap.get("opening_price");
        this.highPrice = (double) linkedHashMap.get("high_price");
        this.lowPrice = (double) linkedHashMap.get("low_price");
        this.tradePrice = (double) linkedHashMap.get("trade_price");
        this.timestamp = (long) linkedHashMap.get("timestamp");
        this.candleAccTradePrice = (double) linkedHashMap.get("candle_acc_trade_price");
        this.candleAccTradeVolume = (double) linkedHashMap.get("candle_acc_trade_volume");
        this.prevClosingPrice = (Double) linkedHashMap.get("prev_closing_price");
        this.changePrice = (Double) linkedHashMap.get("change_price");
        this.changeRate = (Double) linkedHashMap.get("change_rate");
    }
}
