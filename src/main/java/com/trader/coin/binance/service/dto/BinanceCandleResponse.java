package com.trader.coin.binance.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trader.coin.common.service.dto.CandleData;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

import static lombok.AccessLevel.PROTECTED;

@Getter
@NoArgsConstructor
public class BinanceCandleResponse extends CandleData {
    @JsonProperty("Klineopentime")
    public Long KlineOpenTime;
    @JsonProperty("OpenPrice")
    public Double OpenPrice;
    @JsonProperty("Highprice")
    public Double HighPrice;
    @JsonProperty("Lowprice")
    public Double LowPrice;
    @JsonProperty("Closeprice")
    public Double ClosePrice;
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

    public BinanceCandleResponse(List<Object> candleData) {
        if (candleData != null && candleData.size() >= 12) {
            this.KlineOpenTime = (Long) candleData.get(0);
            this.OpenPrice = Double.parseDouble((String) candleData.get(1));
            this.HighPrice = Double.parseDouble((String) candleData.get(2));
            this.LowPrice = Double.parseDouble((String) candleData.get(3));
            this.ClosePrice = Double.parseDouble((String) candleData.get(4));
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
