package com.trader.coin.upbit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.trader.coin.common.Tech.service.TechnicalIndicator;
import com.trader.coin.common.domain.MATrendDirection;
import com.trader.coin.common.infrastructure.ProfitPercentageRepository;
import com.trader.coin.common.infrastructure.alert.discord.DiscordService;
import com.trader.coin.common.infrastructure.config.APIProperties;
import com.trader.coin.common.infrastructure.config.exception.BaseException;
import com.trader.coin.common.infrastructure.config.exception.ErrorCode;
import com.trader.coin.common.service.dto.CandleData;
import com.trader.coin.crypto.infrastructure.jwt.JwtTokenUtil;
import com.trader.coin.upbit.domain.CoinEvaluation;
import com.trader.coin.upbit.domain.ProfitPercentage;
import com.trader.coin.upbit.presentation.dto.CoinInquiryResponse;
import com.trader.coin.upbit.presentation.dto.CoinOrderRequest;
import com.trader.coin.upbit.service.dto.TickerResponse;
import com.trader.coin.upbit.service.dto.UpbitCandleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.trader.coin.common.domain.MATrendDirection.GOLDEN_CROSS;
import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpbitService {
    private final ProfitPercentageRepository profitPercentageRepository;

    private final ObjectMapper objectMapper;
    private final APIProperties api;

    private final JwtTokenUtil jwtTokenUtil;
    private final DiscordService discordService;
    private final TechnicalIndicator technicalIndicator;

    public List<String> findMarkets() {
        WebClient client = WebClient.create(api.getUpbit().getServerUrl());
        String responseBody = client.get()
                .uri("/v1/market/all")
                .retrieve()
                .bodyToMono(String.class)
                .block();
        List<Map<String, String>> responseList;
        try {
            responseList = objectMapper.readValue(responseBody, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "응답 데이터 변환에 실패했습니다.");
        }

        return responseList.stream().map(map -> map.get("market")).toList();
    }

    public List<CandleData> getCandles(String unit, String market, int count) {
        WebClient client = WebClient.create(api.getUpbit().getServerUrl());
        String path = Objects.equals(unit, "days") ? "/days" : "/minutes/" + unit;

        String responseBody = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/candles" + path)
                        .queryParam("market", market)
                        .queryParam("count", count)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        List<CandleData> responseList;
        try {
            responseList = objectMapper.readValue(responseBody, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "응답 데이터 변환에 실패했습니다.");
        }

        return responseList;
    }

    public List<CoinInquiryResponse> getAccountInquiry() {
        String token = jwtTokenUtil.getAuthenticationToken(api);
        WebClient client = WebClient.create(api.getUpbit().getServerUrl());

        String responseBody = client.get()
                .uri("/v1/accounts")
                .header("Content-Type", "application/json")
                .header("Authorization", token)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        List<CoinInquiryResponse> responseList;
        try {
            responseList = objectMapper.readValue(responseBody, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "응답 데이터 변환에 실패했습니다.");
        }

        return responseList;
    }

    // 매수할 코인 평가 및 매수
    @Transactional
    public void waitAndSeeOrderCoin() {
        List<CoinInquiryResponse> inquiries = getAccountInquiry();
        log.warn("업비트 {} 코인 매수 {}캔들 조건 평가 시작", getLocalDateTimeNow(), api.getUpbit_UNIT());

        long KRW_balance = (long) Math.floor(inquiries.stream()
                .filter(inquiry -> inquiry.getCurrency().equals("KRW"))
                .mapToDouble(CoinInquiryResponse::getBalance)
                .findFirst()
                .orElseThrow(() -> new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "KRW 잔고 조회에 실패했습니다.")));

        // 매수할 코인 평가
        List<CoinEvaluation> evaluations = evaluatePotentialBuys();
        if (evaluations.isEmpty()) {
            log.error("매수할 코인이 없습니다.");
            return;
        }

        // 거래량이 높은 코인순으로 정렬 BID_PERCENTAGE
        List<CoinEvaluation> topEvaluations = generateListByAccTracePrice24H(evaluations);
        if (KRW_balance * api.getBID_PERCENTAGE() <= 5_000) {
            log.error("매수 금액이 5000원 이하이므로 매수하지 않습니다.");
            return;
        }

        // 매수
        for (CoinEvaluation coin : topEvaluations) {
            String bidPrice = String.valueOf((long) (KRW_balance * api.getBID_PERCENTAGE()));
//            orderCoin(new CoinOrderRequest(coin.getMarket(), "bid", null, bidPrice, "price"));
            log.info("market: {}, 가격: {} 매수", coin.getMarket(), bidPrice);
        }

        // 손익률 계산
        calculateProfitPercentage();
        delayMethod(1000);
    }

    // 매도할 코인 평가
    @Transactional
    public void evaluateHoldingsForSell() {
        delayMethod(1000);
        List<CoinInquiryResponse> inquiries = getAccountInquiry();
        if (inquiries.size() <= 1) return;
        else log.warn("{} 보유 코인 매도 조건 평가 시작", getLocalDateTimeNow());

        List<ProfitPercentage> profitPercentages = arrangeProfit(inquiries);
        for (CoinInquiryResponse inquiry : inquiries) {
            String market = inquiry.getUnitCurrency() + "-" + inquiry.getCurrency();
            if (inquiry.getCurrency().equals("KRW") || api.getNO_SELL().contains(market)) {
                continue;
            }
            List<CandleData> candles = getCandles(api.getUpbit_UNIT(), market, api.getCANDLE_COUNT());

//            long rsi = technicalIndicator.calculateRSI(candles, api.getRSI_PERIOD());
            double rsi = 0;
            double[] bollingerBands = technicalIndicator.calculateBollingerBand(candles, api.getPERIOD());
            double currentMarketPrice = ((UpbitCandleResponse) candles.get(0)).getTradePrice();
            double avgBuyPrice = inquiry.getAvgBuyPrice();
            double profitAndLossPercentage = (currentMarketPrice - avgBuyPrice) / avgBuyPrice * 100;

            ProfitPercentage profitPercentage = profitPercentages.stream().filter(profit -> profit.getMarket().equals(market))
                    .findFirst().orElseThrow(() -> new BaseException(ErrorCode.NOT_FOUND, "해당 코인의 손익률을 찾을 수 없습니다."));

            // 최대 수익률에서 3% 이상 빠졌는지 확인합니다.
            boolean hasDroppedFromNProfit = profitAndLossPercentage <= profitPercentage.getProfitAndLossPercentage() - api.getAFFORD_LOSS_VALUE();
            boolean hasDroppedFromMAXNProfit = profitAndLossPercentage <= profitPercentage.getProfitAndLossPercentage() - api.getMAX_LOSS_VALUE();
            // 현재 손익률이 -3% 이하인지 확인합니다.
            boolean isLossGreaterThan3Percent = profitAndLossPercentage <= api.getSTOP_LOSS_LINE();
            // 현재 추세가 상승 추세 인지 확인합니다.
            List<Double> priceList = candles.stream()
                    .map(candle -> ((UpbitCandleResponse) candle).getTradePrice())  // 명시적 형변환
                    .toList();
            boolean trendingUp = technicalIndicator.isTrendingUp(priceList, 3);

            // 매도 조건
            if ((rsi >= api.getRSI_SELLING_CONDITION() && ((UpbitCandleResponse) candles.get(0)).getTradePrice() > bollingerBands[0])
                    || isLossGreaterThan3Percent || ((hasDroppedFromNProfit && !trendingUp) || hasDroppedFromMAXNProfit)) {
                orderCoin(new CoinOrderRequest(market, "ask", String.valueOf(inquiry.getBalance()), null, "market"));
                profitPercentageRepository.deleteByMarket(market);
                discordService.sendDiscordAlertLog(market, String.valueOf(((UpbitCandleResponse) candles.get(0)).getTradePrice()),
                        String.valueOf(inquiry.getBalance()), "ask");
                log.info("market: {}, RSI: {}, 볼린저밴드 상단: {}, 볼린저밴드 하단: {}, 손익률: {}%",
                        market,
                        rsi,
                        bollingerBands[0],
                        bollingerBands[1],
                        String.format("%.2f%%", profitAndLossPercentage));
                log.info("매도 조건이 충족되어 매도합니다.");
            } else {
                log.info("market: {}, RSI: {}, 볼린저밴드 상단: {}, 볼린저밴드 하단: {}, 손익률: {}%",
                        market,
                        rsi,
                        bollingerBands[0],
                        bollingerBands[1],
                        String.format("%.2f%%", profitAndLossPercentage));

                if (!api.getNO_SELL().contains(market)) log.info("매도 조건이 충족되지 않아 매도하지 않습니다.");
            }
        }

        calculateProfitPercentage();
    }

    // 손익률 객체 생성
    @Transactional
    public void calculateProfitPercentage() {
        List<CoinInquiryResponse> inquiries = getAccountInquiry();
        arrangeProfit(inquiries);

        String now = getLocalDateTimeNow();
        for (CoinInquiryResponse inquiry : inquiries) {
            if (inquiry.getCurrency().equals("KRW")) {
                continue;
            }
            String market = inquiry.getUnitCurrency() + "-" + inquiry.getCurrency();
            double currentMarketPrice = getTicker(market).getTradePrice();
            double avgBuyPrice = inquiry.getAvgBuyPrice();
            double profitAndLossPercentage = (currentMarketPrice - avgBuyPrice) / avgBuyPrice * 100;
            profitPercentageRepository.findByMarket(market)
                    .ifPresentOrElse(
                            profitPercentage -> {
                                if (profitAndLossPercentage < profitPercentage.getProfitAndLossPercentage()) {
                                    return;
                                }
                                profitPercentage.updateProfitPercentage(profitAndLossPercentage);
                                profitPercentageRepository.save(profitPercentage);
                            },
                            () -> profitPercentageRepository.save(new ProfitPercentage(market, profitAndLossPercentage))
                    );
        }

        log.info("{} 손익률 계산 완료", now);
    }

    // 주문 요청
    public void orderCoin(CoinOrderRequest coinOrderRequest) {
        Map<String, String> params = getOrderParams(coinOrderRequest);
        List<String> queryElements = new ArrayList<>();
        for(Map.Entry<String, String> entity : params.entrySet()) {
            queryElements.add(entity.getKey() + "=" + entity.getValue());
        }
        String queryString = String.join("&", queryElements.toArray(new String[0]));

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-512");
            md.update(queryString.getBytes(UTF_8));
        } catch (Exception e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "해시 생성에 실패했습니다.");
        }

        String queryHash = String.format("%0128x", new BigInteger(1, md.digest()));
        String token = jwtTokenUtil.getAuthenticationToken(api, queryHash);

        // webclient 안됨. httpclient로 대체
        HttpEntity entity;
        try {
            HttpClient client = HttpClientBuilder.create().build();
            HttpPost request = new HttpPost(api.getUpbit().getServerUrl() + "/v1/orders");
            request.setHeader("Content-Type", "application/json");
            request.addHeader("Authorization", token);
            request.setEntity(new StringEntity(new Gson().toJson(params)));

            HttpResponse response = client.execute(request);
            entity = response.getEntity();

            String message = EntityUtils.toString(entity, "UTF-8");
            if (message.contains("error")) {
                throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, message);
            }
        } catch (IOException e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "주문 요청에 실패했습니다.");
        }

        discordService.sendDiscordAlertLog(
                coinOrderRequest.getMarket(), coinOrderRequest.getPrice(),
                coinOrderRequest.getVolume(), coinOrderRequest.getSide());
    }

    private List<ProfitPercentage> arrangeProfit(List<CoinInquiryResponse> inquiries) {
        List<ProfitPercentage> percentages = profitPercentageRepository.findAll();
        List<String> markets = inquiries.stream()
                .filter(inquiry -> !inquiry.getCurrency().equals("KRW"))
                .map(inquiry -> inquiry.getUnitCurrency() + "-" + inquiry.getCurrency())
                .toList();
        List<ProfitPercentage> toDelete = percentages.stream()
                .filter(profitPercentage -> !markets.contains(profitPercentage.getMarket()))
                .toList();

        toDelete.forEach(profitPercentageRepository::delete);
        percentages.removeAll(toDelete);
        return percentages;
    }

    private TickerResponse getTicker(String market) {
        WebClient client = WebClient.create(api.getUpbit().getServerUrl());
        String responseBody = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/ticker")
                        .queryParam("markets", market)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        List<TickerResponse> ticker;
        try {
            ticker = objectMapper.readValue(responseBody, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "응답 데이터 변환에 실패했습니다.");
        }

        return ticker.get(0);
    }

    // 매수할 코인 평가
    private List<CoinEvaluation> evaluatePotentialBuys() {
        List<CoinEvaluation> evaluations = new ArrayList<>();
        for (String market : findMarkets()) {
            if (!market.contains("KRW") || api.getNO_BUY().contains(market)) {
                continue;
            }

            List<CandleData> candles = getCandles(api.getUpbit_UNIT(), market, api.getCANDLE_COUNT());
            if (candles.size() < api.getCANDLE_COUNT()) {
                continue;
            }

//            long rsi = technicalIndicator.calculateRSI(candles, api.getRSI_PERIOD());
            long rsi = 0;
            double[] bollingerBand = technicalIndicator.calculateBollingerBand(candles, api.getPERIOD());

            // 매수 조건
            // 볼린저 밴드 하단 범위 설정. 하단 돌파부터 근접
//            double lowerBand = bollingerBand[1];
//            double lowerBandNear = lowerBand * 1.05; // 볼린저 밴드 하단의 5% 위
//            boolean isBandTrue = candles.get(0).getTradePrice() <= lowerBandNear && candles.get(0).getTradePrice() >= lowerBand;

            double currentTradePrice = ((UpbitCandleResponse) candles.get(0)).getTradePrice();
            boolean isPriceDown = currentTradePrice <= ((UpbitCandleResponse) candles.get(1)).getTradePrice() * 0.95;
            boolean isBandTrue = currentTradePrice <= bollingerBand[1] && currentTradePrice < bollingerBand[0];
            MATrendDirection goldenCross = technicalIndicator.isGoldenCross(candles);


            if ((rsi < api.getRSI_BUYING_CONDITION() && isBandTrue && !isPriceDown) || (goldenCross == GOLDEN_CROSS && !(rsi > api.getRSI_SELLING_CONDITION()))) {
                evaluations.add(new CoinEvaluation(market, rsi, currentTradePrice, bollingerBand));
            }

            delayMethod(300);
        }
        return evaluations;
    }

    // 매수할 코인 리스트 24시간 거래량 순으로 정렬
    private List<CoinEvaluation> generateListByAccTracePrice24H(List<CoinEvaluation> evaluations) {
        evaluations.forEach(
                evaluation -> {
                    double accTradePrice24h = getTicker(evaluation.getMarket()).getAccTradePrice24h();
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

    private Map<String, String> getOrderParams(CoinOrderRequest coinOrderRequest) {
        Map<String, String> params = new HashMap<>();
        if (coinOrderRequest.getVolume() == null) {
            params.put("market", coinOrderRequest.getMarket());
            params.put("side", coinOrderRequest.getSide());
            params.put("price", coinOrderRequest.getPrice());
            params.put("ord_type", coinOrderRequest.getOrd_type());
        } else {
            params.put("market", coinOrderRequest.getMarket());
            params.put("side", coinOrderRequest.getSide());
            params.put("volume", coinOrderRequest.getVolume());
            params.put("ord_type", coinOrderRequest.getOrd_type());
        }
        return params;
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
