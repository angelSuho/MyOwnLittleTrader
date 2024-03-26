package com.trader.coin.common.Tech.service;

import com.trader.coin.upbit.service.dto.CandleResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class TechnicalIndicator {

    public double calculateSMA(List<CandleResponse> candles, int period) {
        double sum = 0;
        for (int i = 0; i < period; i++) {
            sum += candles.get(i).getTradePrice();
        }
        return sum / period;
    }

    public double calculateEMA(List<CandleResponse> values, int period) {
        double a = 2.0 / (period + 1);
        double ema = values.get(0).getTradePrice(); // starting with the first value

        for (int i = 1; i < values.size(); i++) {
            ema = ((values.get(i).getTradePrice() - ema) * a) + ema;
        }

        return ema;
    }

    public double calculateStandardDeviation(List<CandleResponse> candles, double sma, int period) {
        double variance = 0.0;
        for (int i = 0; i < period; i++) {
            variance += Math.pow(candles.get(i).getTradePrice() - sma, 2);
        }
        return Math.sqrt(variance / period);
    }

    public double calculateUpbitRSI(List<CandleResponse> candles, int period) {
        candles = candles.stream()
                .sorted(Comparator.comparing(CandleResponse::getTimestamp))
                .toList();

        double zero = 0;
        List<Double> upList = new ArrayList<>();
        List<Double> downList = new ArrayList<>();
        for (int i = 0; i < candles.size() - 1; i++) {
            double gapByTradePrice = candles.get(i + 1).getTradePrice() - candles.get(i).getTradePrice();
            if (gapByTradePrice > 0) {
                upList.add(gapByTradePrice);
                downList.add(zero);
            } else if (gapByTradePrice < 0) {
                downList.add(gapByTradePrice * -1);
                upList.add(zero);
            } else {
                upList.add(zero);
                downList.add(zero);
            }
        }

        double a = (double) 1 / (1 + (period - 1));
        double upEma = 0;
        if (!upList.isEmpty()) {
            upEma = upList.get(0);
            if (upList.size() > 1) {
                for (int i = 1 ; i < upList.size(); i++) {
                    upEma = (upList.get(i) * a) + (upEma * (1 - a));
                }
            }
        }

        double downEma = 0;
        if (!downList.isEmpty()) {
            downEma = downList.get(0);
            if (downList.size() > 1) {
                for (int i = 1; i < downList.size(); i++) {
                    downEma = (downList.get(i) * a) + (downEma * (1 - a));
                }
            }
        }

        double au = upEma;
        double ad = downEma;
        double rs = au / ad;

        return 100 - (100 / (1 + rs));
    }

    public double[] calculateMACD(List<CandleResponse> candles, int shortPeriod, int longPeriod, int signalPeriod) {
        double shortEMA = calculateEMA(candles, shortPeriod);
        double longEMA = calculateEMA(candles, longPeriod);
        double macd = shortEMA - longEMA;
        double signal = calculateEMA(candles, signalPeriod);
        double histogram = macd - signal;
        return new double[]{macd, signal, histogram};
    }

    public long[] calculateBollingerBand(List<CandleResponse> candles, int period) {
        double sma = calculateSMA(candles, period);
        double standardDeviation = calculateStandardDeviation(candles, sma, period);
        double upperBand = sma + 2 * standardDeviation;
        double lowerBand = sma - 2 * standardDeviation;
        return new long[]{(long)upperBand, (long)lowerBand};
    }
}
