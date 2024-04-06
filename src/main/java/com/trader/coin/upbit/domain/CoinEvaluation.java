package com.trader.coin.upbit.domain;

import lombok.Getter;

@Getter
public class CoinEvaluation {
    private final String market;
    private final long rsi;
    private final double tradePrice;
    private final double[] bollingerBand;
    private double accTradePrice24h;

    public CoinEvaluation(String market, long rsi, double tradePrice, double[] bollingerBand) {
        this.market = market;
        this.rsi = rsi;
        this.tradePrice = tradePrice;
        this.bollingerBand = bollingerBand;
    }

    public void initAccTradePrice24h(double accTradePrice24h) {
        this.accTradePrice24h = accTradePrice24h;
    }
}
