package com.trader.coin.binance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trader.coin.binance.service.dto.CandleResponse;
import com.trader.coin.binance.service.dto.MarketResponse;
import com.trader.coin.binance.service.dto.TickerResponse;
import com.trader.coin.common.Tech.service.TechnicalIndicator;
import com.trader.coin.common.infrastructure.ProfitPercentageRepository;
import com.trader.coin.common.infrastructure.alert.discord.DiscordService;
import com.trader.coin.common.infrastructure.config.APIProperties;
import com.trader.coin.common.infrastructure.config.exception.BaseException;
import com.trader.coin.common.infrastructure.config.exception.ErrorCode;
import com.trader.coin.crypto.infrastructure.jwt.JwtTokenUtil;
import com.trader.coin.upbit.domain.CoinEvaluation;
import com.trader.coin.upbit.presentation.dto.CoinInquiryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BinanceService {
    private final ProfitPercentageRepository profitPercentageRepository;

    private final ObjectMapper objectMapper;

    private final APIProperties api;
    private final JwtTokenUtil jwtTokenUtil;
    private final DiscordService discordService;
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

    public List<CandleResponse> getCandles(String market) {
        WebClient client = WebClient.create(api.getBinance().getServerUrl());

        String responseBody = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v3/klines")
                        .queryParam("symbol", market + "USDT")
                        .queryParam("interval", api.getBinance_INTERVAL())
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        List<CandleResponse> responseList = new ArrayList<>();
        try {
            List<List<Object>> rawResponse = objectMapper.readValue(responseBody, new TypeReference<>() {});
            for (List<Object> rawData : rawResponse) {
                responseList.add(new CandleResponse(rawData));
            }
        } catch (JsonProcessingException e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "응답 데이터 변환에 실패했습니다.");
        }

        return responseList;
    }

//    @Transactional
//    public void waitAndSeeOrderCoin() {
//        List<CoinInquiryResponse> inquiries = getAccountInquiry();
//        log.warn("업비트 {} 코인 매수 {}캔들 조건 평가 시작", getLocalDateTimeNow(), api.getUpbit_UNIT());
//
//        long KRW_balance = (long) Math.floor(inquiries.stream()
//                .filter(inquiry -> inquiry.getCurrency().equals("KRW"))
//                .mapToDouble(CoinInquiryResponse::getBalance)
//                .findFirst()
//                .orElseThrow(() -> new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "KRW 잔고 조회에 실패했습니다.")));
//
//        // 매수할 코인 평가
//        List<CoinEvaluation> evaluations = evaluatePotentialBuys();
//        if (evaluations.isEmpty()) {
//            log.error("매수할 코인이 없습니다.");
//            return;
//        }
//
//        // 거래량이 높은 코인순으로 정렬 BID_PERCENTAGE
//        List<CoinEvaluation> topEvaluations = generateListByAccTracePrice24H(evaluations);
//        if (KRW_balance * api.getBID_PERCENTAGE() <= 5_000) {
//            log.error("매수 금액이 5000원 이하이므로 매수하지 않습니다.");
//            return;
//        }
//
//        // 매수
//        for (CoinEvaluation coin : topEvaluations) {
//            String bidPrice = String.valueOf((long) (KRW_balance * api.getBID_PERCENTAGE()));
////            orderCoin(new CoinOrderRequest(coin.getMarket(), "bid", null, bidPrice, "price"));
//            log.info("market: {}, 가격: {} 매수", coin.getMarket(), bidPrice);
//        }
//
//        delayMethod(1000);
//    }

//    private List<CoinEvaluation> generateListByAccTracePrice24H(List<CoinEvaluation> evaluations) {
//        evaluations.forEach(
//                evaluation -> {
//                    double accTradePrice24h = getTicker(evaluation.getMarket()).getAccTradePrice24h();
//                    evaluation.initAccTradePrice24h(accTradePrice24h);
//                    delayMethod(100);
//                }
//        );
//        List<CoinEvaluation> sortedEvaluations = evaluations.stream()
//                .sorted(Comparator.comparingDouble(CoinEvaluation::getAccTradePrice24h).reversed())
//                .toList();
//        sortedEvaluations.forEach(evaluation -> log.info("market: {}, 거래량: {}", evaluation.getMarket(), evaluation.getAccTradePrice24h()));
//
//        return sortedEvaluations.stream()
//                .limit(api.getNUMBER_OF_BUY())
//                .toList();
//    }

    public double getTicker() {
        List<String> markets = List.of("BTCUSDT", "ETHUSDT");
        WebClient client = WebClient.create(api.getBinance().getServerUrl());
        for (String market : markets) {
            String responseBody = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v3/ticker/24hr")
                            .queryParam("symbol", market + "USDT")
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            List<TickerResponse> responseList;
            try {
                responseList = objectMapper.readValue(responseBody, new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "응답 데이터 변환에 실패했습니다.");
            }
            System.out.println(responseList.get(0).getVolume());
        }
        return 0;
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