package com.trader.coin.binance.service.dto;

import lombok.Getter;

@Getter
public enum Interval {
    FIFTEEN_MINUTES("15m"),
    THIRTY_MINUTES("30m"),
    ONE_HOUR("1h"),
    FOUR_HOURS("4h"),
    ONE_DAY("1d");

    private final String value;

    Interval(String value) {
        this.value = value;
    }

    public static Interval fromString(String value) {
        for (Interval interval : Interval.values()) {
            if (interval.value.equals(value)) {
                return interval;
            }
        }
        throw new IllegalArgumentException("No constant with value " + value + " found");
    }
}
