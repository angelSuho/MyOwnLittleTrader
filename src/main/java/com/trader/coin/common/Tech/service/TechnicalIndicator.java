package com.trader.coin.common.Tech.service;

import com.trader.coin.binance.service.dto.BinanceCandleResponse;
import com.trader.coin.common.domain.MATrendDirection;
import com.trader.coin.common.domain.Market;
import com.trader.coin.common.infrastructure.config.APIProperties;
import com.trader.coin.common.infrastructure.config.exception.BaseException;
import com.trader.coin.common.infrastructure.config.exception.ErrorCode;
import com.trader.coin.common.service.dto.CandleData;
import com.trader.coin.upbit.service.dto.UpbitCandleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TechnicalIndicator {
    private final APIProperties api;

    public List<Double> calculateSMAList(List<? extends CandleData> candles, int period) {

        List<Double> movingAverages = new ArrayList<>();
        for (int i = period - 1; i < candles.size(); i++) {
            double sum = 0;
            for (int j = i - (period - 1); j <= i; j++) {
                if (candles.get(j) instanceof BinanceCandleResponse) {
                    sum += ((BinanceCandleResponse) candles.get(j)).getClosePrice();
                } else if (candles.get(j) instanceof UpbitCandleResponse) {
                    sum += ((UpbitCandleResponse) candles.get(j)).getTradePrice();
                }
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

    public MATrendDirection isGoldenCross(List<CandleData> candles) {
        List<Double> MA15 = calculateSMAList(candles, 15);
        List<Double> MA50 = calculateSMAList(candles, 50);
        List<Double> bandMiddleLineList = calculateBollingerBandMiddleLineList(candles, 20);

        if (MA15.size() < 2 || MA50.size() < 2) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "ShortMA and LongMA must have at least 2 elements");
        }

        boolean hasCrossed = MA15.get(0) > MA50.get(0) && MA15.get(1) <= MA50.get(1);
        boolean is15maTrendingUp = isTrendingUp(MA15, 3);
        boolean is15maCrossedMiddleLine = MA15.get(0) > bandMiddleLineList.get(0) && MA15.get(1) <= bandMiddleLineList.get(1);

        if (hasCrossed && is15maTrendingUp || is15maCrossedMiddleLine) {
            return MATrendDirection.GOLDEN_CROSS;
        } else {
            return MATrendDirection.FLAT;
        }
    }

    public double calculateSMA(List<CandleData> candles, int period) {
        double sum = 0;
        for (int i = 0; i < period; i++) {
            if (candles.get(i) instanceof BinanceCandleResponse) {
                sum += ((BinanceCandleResponse) candles.get(i)).getClosePrice();
            } else if (candles.get(i) instanceof UpbitCandleResponse) {
                sum += ((UpbitCandleResponse) candles.get(i)).getTradePrice();
            }
        }
        return sum / period;
    }

    public double calculateEMA(List<UpbitCandleResponse> values, int period) {
        double a = 2.0 / (period + 1);
        double ema = values.get(0).getTradePrice(); // starting with the first value

        for (int i = 1; i < values.size(); i++) {
            ema = ((values.get(i).getTradePrice() - ema) * a) + ema;
        }

        return ema;
    }

    public double calculateStandardDeviation(List<CandleData> candles, double sma, int period) {
        double variance = 0.0;
        for (int i = 0; i < period; i++) {
            if (candles.get(i) instanceof BinanceCandleResponse) {
                variance += Math.pow(((BinanceCandleResponse) candles.get(i)).getClosePrice() - sma, 2);
            } else if (candles.get(i) instanceof UpbitCandleResponse) {
                variance += Math.pow(((UpbitCandleResponse) candles.get(i)).getTradePrice() - sma, 2);
            }
        }
        return Math.sqrt(variance / period);
    }

    public long calculateRSI(List<CandleData> candles, int period) {
        List<? extends CandleData> candleList = candles;
        if (candles.get(0) instanceof BinanceCandleResponse) {
            candleList = candles.stream()
                    .map(candle -> (BinanceCandleResponse) candle)  // 명시적 형변환
                    .sorted(Comparator.comparing(BinanceCandleResponse::getKlineOpenTime))
                    .toList();
        } else if (candles.get(0) instanceof UpbitCandleResponse) {
            candleList = candles.stream()
                    .map(candle -> (UpbitCandleResponse) candle)  // 명시적 형변환
                    .sorted(Comparator.comparing(UpbitCandleResponse::getTimestamp))
                    .toList();
        }

        double zero = 0;
        List<Double> upList = new ArrayList<>();
        List<Double> downList = new ArrayList<>();
        for (int i = 0; i < candles.size() - 1; i++) {
            double gapByTradePrice = getGapByTradePrice(candles, i, candleList);

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

        return (long) Math.floor(100 - (100 / (1 + rs)));
    }

    public double[] calculateBollingerBand(List<CandleData> candles, int period) {
        double sma = calculateSMA(candles, period);
        double standardDeviation = calculateStandardDeviation(candles, sma, period);
        double upperBand = sma + 2 * standardDeviation;
        double lowerBand = sma - 2 * standardDeviation;
        return new double[]{upperBand, lowerBand, sma};
    }

    public List<Double> calculateBollingerBandMiddleLineList(List<CandleData> candles, int period) {
        List<Double> middleLines = new ArrayList<>();
        for (int i = 0; i <= candles.size() - period; i++) {
            List<CandleData> subList = candles.subList(i, i + period);
            double sma = calculateSMA(subList, period); // 이미 선언된 메소드 사용
            middleLines.add(sma); // 계산된 SMA 값을 반올림하여 long 타입으로 변환 후 추가
        }
        return middleLines;
    }

    public boolean isCandleRising(List<UpbitCandleResponse> candles, int n) {
        if (candles.size() < 2) {
            return false;
        }
        for (int i = 0; i < 2; i++) {
            if (isPriceDifferenceOverNPercent(candles.get(i).getHighPrice(), candles.get(i).getLowPrice(), n)) {
                return false;
            }
        }
        return true;
    }

    private static double getGapByTradePrice(List<CandleData> candles, int i, List<? extends CandleData> candleList) {
        double gapByTradePrice;
        if (candles.get(i) instanceof BinanceCandleResponse)
            gapByTradePrice = ((BinanceCandleResponse) candleList.get(i + 1)).getClosePrice() - ((BinanceCandleResponse) candleList.get(i)).getClosePrice();
        else if (candles.get(i) instanceof UpbitCandleResponse)
            gapByTradePrice = ((UpbitCandleResponse) candleList.get(i + 1)).getTradePrice() - ((UpbitCandleResponse) candleList.get(i)).getTradePrice();
        else {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Candle type is not supported");
        }
        return gapByTradePrice;
    }

    private boolean isPriceDifferenceOverNPercent(double highPrice, double lowPrice, int n) {
        double difference = highPrice - lowPrice;
        double percentageDifference = (difference / lowPrice) * 100;
        return percentageDifference >= n;
    }
}
