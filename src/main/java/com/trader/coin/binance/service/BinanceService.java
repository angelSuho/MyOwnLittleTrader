package com.trader.coin.binance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trader.coin.binance.service.dto.BinanceCandleResponse;
import com.trader.coin.binance.service.dto.MarketResponse;
import com.trader.coin.binance.service.dto.TickerResponse;
import com.trader.coin.common.Tech.service.TechnicalIndicator;
import com.trader.coin.common.domain.MATrendDirection;
import com.trader.coin.common.infrastructure.config.APIProperties;
import com.trader.coin.common.infrastructure.config.exception.BaseException;
import com.trader.coin.common.infrastructure.config.exception.ErrorCode;
import com.trader.coin.common.service.dto.CandleData;
import com.trader.coin.upbit.domain.CoinEvaluation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.trader.coin.common.domain.MATrendDirection.GOLDEN_CROSS;

@Slf4j
@Service
@RequiredArgsConstructor
public class BinanceService {

    private final ObjectMapper objectMapper;

    private final APIProperties api;
    private final TechnicalIndicator technicalIndicator;

    public List<MarketResponse> getMarkets() {
        WebClient client = WebClient.builder()
                .baseUrl(api.getBinance().getServerUrl())
                .build();

        List<MarketResponse> marketResponses;
        String response = client.get()
                .uri("/api/v3/ticker/price")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            marketResponses = objectMapper.readValue(response, new TypeReference<>() {});
            marketResponses = marketResponses.stream()
                    .filter(market -> market.getSymbol().contains("USDT"))
                    .toList();

        } catch (JsonProcessingException e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "응답 데이터 변환에 실패했습니다.");
        }
        return marketResponses;
    }

    public List<CandleData> getCandles(String market) {
        String serverUrl = api.isFutures() ? api.getBinance().getFuturesUrl() + "/fapi/v1/klines" :
                api.getBinance().getServerUrl() + "/api/v3/klines";
        WebClient client = WebClient.create(serverUrl);

        String responseBody = client.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("symbol", market)
                        .queryParam("interval", api.getBinance_INTERVAL())
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        List<CandleData> responseList = new ArrayList<>();
        try {
            List<List<Object>> rawResponse = objectMapper.readValue(responseBody, new TypeReference<>() {});
            for (List<Object> rawData : rawResponse) {
                responseList.add(new BinanceCandleResponse(rawData));
            }
        } catch (JsonProcessingException e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "응답 데이터 변환에 실패했습니다.");
        }

        return responseList;
    }

    @Transactional
    public void waitAndSeeOrderCoin() {
        log.warn("바이낸스 {} 코인 매수 {}캔들 조건 평가 시작", getLocalDateTimeNow(), api.getBinance_INTERVAL());

        // 매수할 코인 평가
        List<CoinEvaluation> evaluations = evaluatePotentialBuys();
        if (evaluations.isEmpty()) {
            log.error("매수할 코인이 없습니다.");
            return;
        }

        // 거래량이 높은 코인순으로 정렬 BID_PERCENTAGE
        List<CoinEvaluation> topEvaluations = generateListByAccTracePrice24H(evaluations);
        delayMethod(1000);
    }

    private List<CoinEvaluation> evaluatePotentialBuys() {
        List<CoinEvaluation> evaluations = new ArrayList<>();
        for (String market : api.getBUY_FUTURES()) {
            List<CandleData> candles = getCandles(market);

            long rsi = technicalIndicator.calculateRSI(candles, api.getRSI_PERIOD());
            double[] bollingerBand = technicalIndicator.calculateBollingerBand(candles, api.getPERIOD());

            // 매수 조건
            // 볼린저 밴드 하단 범위 설정. 하단 돌파부터 근접
//            double lowerBand = bollingerBand[1];
//            double lowerBandNear = lowerBand * 1.05; // 볼린저 밴드 하단의 5% 위
//            boolean isBandTrue = candles.get(0).getTradePrice() <= lowerBandNear && candles.get(0).getTradePrice() >= lowerBand;

            double currentTradePrice = ((BinanceCandleResponse) candles.get(0)).getClosePrice();
            boolean isPriceDown = currentTradePrice <= ((BinanceCandleResponse) candles.get(1)).getClosePrice() * 0.95;
            boolean isBandTrue = currentTradePrice <= bollingerBand[1] && currentTradePrice < bollingerBand[0];
            MATrendDirection goldenCross = technicalIndicator.isGoldenCross(candles);

            if ((rsi < api.getRSI_BUYING_CONDITION() && isBandTrue && !isPriceDown) || (goldenCross == GOLDEN_CROSS && !(rsi > api.getRSI_SELLING_CONDITION()))) {
                evaluations.add(new CoinEvaluation(market, rsi, currentTradePrice, bollingerBand));
            }

            delayMethod(300);
        }

        return evaluations;
    }

    private List<CoinEvaluation> generateListByAccTracePrice24H(List<CoinEvaluation> evaluations) {
        evaluations.forEach(
                evaluation -> {
                    double accTradePrice24h = Double.parseDouble(getTicker(evaluation.getMarket()).getQuoteVolume());
                    evaluation.initAccTradePrice24h(accTradePrice24h);
                    delayMethod(100);
                }
        );
        List<CoinEvaluation> sortedEvaluations = evaluations.stream()
                .sorted(Comparator.comparingDouble(CoinEvaluation::getAccTradePrice24h).reversed())
                .toList();
        sortedEvaluations.forEach(evaluation -> log.info("market: {}, 거래량: {}", evaluation.getMarket(), evaluation.getAccTradePrice24h()));

        return sortedEvaluations.stream()
                .limit(api.getNUMBER_OF_BUY())
                .toList();
    }

    public TickerResponse getTicker(String market) {
        String serverUrl = api.isFutures() ? api.getBinance().getFuturesUrl() + "/fapi/v1/ticker/24hr?symbol=" :
                api.getBinance().getServerUrl() + "/api/v3/ticker/24hr?symbol=";
        HttpClient client = HttpClientBuilder.create().build();

        HttpGet request = new HttpGet(serverUrl + market);
        request.setHeader("Content-Type", "application/json");

        TickerResponse ticker;
        try {
            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();
            String message = EntityUtils.toString(entity, "UTF-8");
            ticker = objectMapper.readValue(message, new TypeReference<>() {});
        } catch (IOException e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "응답 데이터 변환에 실패했습니다.");
        }

        return ticker;
    }

    private void delayMethod(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "요청 대기 중 오류가 발생했습니다.");
        }
    }

    private String getLocalDateTimeNow() {
        return DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분 ss초").format(LocalDateTime.now());
    }
}