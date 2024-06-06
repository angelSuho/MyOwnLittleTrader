package com.trader.coin.common.domain;

import lombok.Getter;

@Getter
public enum Market {
    UPBIT("UPBIT"),
    BINANCE("BINANCE");

    private final String market;

    Market(String market) {
        this.market = market;
    }
}
