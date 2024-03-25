package com.trader.coin.upbit.domain;

import lombok.Getter;

@Getter
public class CoinEvaluation implements Comparable<CoinEvaluation> {
    private final String market;
    private final double rsi;
    private final double tradePrice;
    private final double lowerBollingerBand;

    public CoinEvaluation(String market, double rsi, double tradePrice, double lowerBollingerBand) {
        this.market = market;
        this.rsi = rsi;
        this.tradePrice = tradePrice;
        this.lowerBollingerBand = lowerBollingerBand;
    }

    @Override
    public int compareTo(CoinEvaluation o) {
        return 0;
    }
}
