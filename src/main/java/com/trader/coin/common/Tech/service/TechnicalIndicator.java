package com.trader.coin.common.Tech.service;

import com.trader.coin.upbit.presentation.CandleResponse;
import org.springframework.stereotype.Component;

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

    public double calculateStandardDeviation(List<CandleResponse> candles, double sma, int period) {
        double variance = 0.0;
        for (int i = 0; i < period; i++) {
            variance += Math.pow(candles.get(i).getTradePrice() - sma, 2);
        }
        return Math.sqrt(variance / period);
    }

    public double calculateRSI(List<CandleResponse> candles, int period) {
        double avgGain = 0;
        double avgLoss = 0;
        for (int i = 1; i < period; i++) {
            double delta = candles.get(i).getTradePrice() - candles.get(i - 1).getTradePrice();
            if (delta > 0) {
                avgGain += delta;
            } else {
                avgLoss -= delta;
            }
        }
        avgGain /= period;
        avgLoss /= period;
        return 100 - (100 / (1 + avgGain / avgLoss));
    }

    public double calculateMACD(List<CandleResponse> candles, int shortPeriod, int longPeriod) {
        return calculateSMA(candles, shortPeriod) - calculateSMA(candles, longPeriod);
    }

    public double[] calculateBollingerBand(List<CandleResponse> candles, int period) {
        double sma = calculateSMA(candles, period);
        double standardDeviation = calculateStandardDeviation(candles, sma, period);
        double upperBand = sma + 2 * standardDeviation;
        double lowerBand = sma - 2 * standardDeviation;
        return new double[]{upperBand, lowerBand};
    }
}
