package com.trader.coin.common.Tech.service;

import com.trader.coin.common.domain.MATrendDirection;
import com.trader.coin.common.infrastructure.config.exception.BaseException;
import com.trader.coin.common.infrastructure.config.exception.ErrorCode;
import com.trader.coin.upbit.service.dto.CandleResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class TechnicalIndicator {
    public List<Double> calculateSMAList(List<CandleResponse> candles, int period) {
        List<Double> movingAverages = new ArrayList<>();
        for (int i = period - 1; i < candles.size(); i++) {
            double sum = 0;
            for (int j = i - (period - 1); j <= i; j++) {
                sum += candles.get(j).getTradePrice();
            }
            movingAverages.add(sum / period);
        }
        return movingAverages;
    }

    public boolean isTrendingUp(List<Double> maValues, int period) {
        for (int i = 0; i < period; i++) {
            if (maValues.get(i) <= maValues.get(i + 1)) {
                return false;
            }
        }
        return true;
    }

    public MATrendDirection isGoldenCross(List<CandleResponse> candles) {
        List<Double> MA15 = calculateSMAList(candles, 15);
        List<Double> MA50 = calculateSMAList(candles, 50);
        List<Double> bandMiddleLineList = calculateBollingerBandMiddleLineList(candles, 20);

        if (MA15.size() < 2 || MA50.size() < 2) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "ShortMA and LongMA must have at least 2 elements");
        }

        boolean hasCrossed = MA15.get(0) > MA50.get(0) && MA15.get(1) <= MA50.get(1);
        boolean is15maTrendingUp = isTrendingUp(MA15, 4);
        boolean isMiddleLineTrendingUp = isTrendingUp(bandMiddleLineList, 4);
        boolean is15maCrossedMiddleLine = MA15.get(0) > bandMiddleLineList.get(0) && MA15.get(1) <= bandMiddleLineList.get(1);

        if (hasCrossed && is15maTrendingUp || is15maCrossedMiddleLine && isMiddleLineTrendingUp) {
            return MATrendDirection.GOLDEN_CROSS;
        } else {
            return MATrendDirection.FLAT;
        }
    }

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

    public long[] calculateBollingerBand(List<CandleResponse> candles, int period) {
        double sma = calculateSMA(candles, period);
        double standardDeviation = calculateStandardDeviation(candles, sma, period);
        double upperBand = sma + 2 * standardDeviation;
        double lowerBand = sma - 2 * standardDeviation;
        return new long[]{(long)upperBand, (long)lowerBand, (long)sma};
    }

    public List<Double> calculateBollingerBandMiddleLineList(List<CandleResponse> candles, int period) {
        List<Double> middleLines = new ArrayList<>();
        for (int i = 0; i <= candles.size() - period; i++) {
            List<CandleResponse> subList = candles.subList(i, i + period);
            double sma = calculateSMA(subList, period); // 이미 선언된 메소드 사용
            middleLines.add(sma); // 계산된 SMA 값을 반올림하여 long 타입으로 변환 후 추가
        }
        return middleLines;
    }
}
