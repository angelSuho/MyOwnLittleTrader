package com.trader.coin.binance.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
public class CandleResponse {
    @JsonProperty("Klineopentime")
    public Long KlineOpenTime;
    @JsonProperty("OpenPrice")
    public String OpenPrice;
    @JsonProperty("Highprice")
    public String HighPrice;
    @JsonProperty("Lowprice")
    public String LowPrice;
    @JsonProperty("Closeprice")
    public String ClosePrice;
    @JsonProperty("Volume")
    public String Volume;
    @JsonProperty("Klineclosetime")
    public Long KlineCloseTime;
    @JsonProperty("Quoteassetvolume")
    public String QuoteAssetVolume;
    @JsonProperty("Numberoftrades")
    public int NumberOfTrades;
    @JsonProperty("Takerbuybaseassetvolume")
    public String TakerBuyBaseAssetVolume;
    @JsonProperty("Takerbuyquoteassetvolume")
    public String TakerBuyQuoteAssetVolume;
    @JsonProperty("Ignore")
    public String Ignore;

    public CandleResponse(List<Object> candleData) {
        if (candleData != null && candleData.size() >= 12) {
            this.KlineOpenTime = (Long) candleData.get(0);
            this.OpenPrice = (String) candleData.get(1);
            this.HighPrice = (String) candleData.get(2);
            this.LowPrice = (String) candleData.get(3);
            this.ClosePrice = (String) candleData.get(4);
            this.Volume = (String) candleData.get(5);
            this.KlineCloseTime = (Long) candleData.get(6);
            this.QuoteAssetVolume = (String) candleData.get(7);
            this.NumberOfTrades = (Integer) candleData.get(8);
            this.TakerBuyBaseAssetVolume = (String) candleData.get(9);
            this.TakerBuyQuoteAssetVolume = (String) candleData.get(10);
            this.Ignore = (String) candleData.get(11);
        }
    }
}
