package com.trader.coin.upbit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.trader.coin.common.Tech.service.TechnicalIndicator;
import com.trader.coin.common.infrastructure.alert.discord.DiscordService;
import com.trader.coin.common.infrastructure.config.exception.BaseException;
import com.trader.coin.common.infrastructure.config.exception.ErrorCode;
import com.trader.coin.crypto.infrastructure.jwt.JwtTokenUtil;
import com.trader.coin.upbit.domain.CoinEvaluation;
import com.trader.coin.upbit.infrastructure.config.UpbitProperties;
import com.trader.coin.upbit.presentation.CandleResponse;
import com.trader.coin.upbit.presentation.CoinInquiryResponse;
import com.trader.coin.upbit.presentation.CoinOrderRequest;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
@RequiredArgsConstructor
public class UpbitService {

    private final ObjectMapper objectMapper;
    private final UpbitProperties upbitProperties;

    private final JwtTokenUtil jwtTokenUtil;
    private final DiscordService discordService;
    private final TechnicalIndicator technicalIndicator;

    public List<String> findMarkets() {
        WebClient client = WebClient.create(upbitProperties.getServerUrl());
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

    public List<CandleResponse> getCandles(int unit, String market, int count) {
        WebClient client = WebClient.create(upbitProperties.getServerUrl());
        String responseBody = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/candles/minutes/" + unit)
                        .queryParam("market", market)
                        .queryParam("count", count)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        List<CandleResponse> responseList;
        try {
            responseList = objectMapper.readValue(responseBody, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "응답 데이터 변환에 실패했습니다.");
        }

        return responseList;
    }

    public List<CoinInquiryResponse> getAccountInquiry() {
        String token = jwtTokenUtil.getAuthenticationToken(upbitProperties);
        WebClient client = WebClient.create(upbitProperties.getServerUrl());

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

    @Scheduled(cron = "0 0 */4 * * *")
    public void waitAndSeeOrderCoin() {
        int unit = 240;
        double bidPercentage = 0.25;
        List<CoinInquiryResponse> inquiries = getAccountInquiry();

        // KRW 제외하고 보유 코인 3개 일 경우 매수 안함
        if (inquiries.size() >= 3) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "보유 코인이 3개 이상일 경우 매수하지 않습니다.");
        }

        List<String> markets = findMarkets();
        long krwBalance = (long) Math.floor(inquiries.stream()
                .filter(inquiry -> inquiry.getCurrency().equals("KRW"))
                .mapToDouble(CoinInquiryResponse::getBalance)
                .findFirst()
                .orElseThrow(() -> new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "KRW 잔고 조회에 실패했습니다.")));
        delayMethod(150);

        List<CoinEvaluation> evaluations = new ArrayList<>();
        for (String market : markets) {
            if (!market.contains("KRW")) {
                continue;
            }

            List<CandleResponse> candles = getCandles(unit, market, 20);
            double rsi = technicalIndicator.calculateRSI(candles, 14);
            double[] bollingerBand = technicalIndicator.calculateBollingerBand(candles, 20);
            
            // 매수 조건
            if (rsi < 30 || candles.get(0).getTradePrice() < bollingerBand[1]) {
                evaluations.add(new CoinEvaluation(market, rsi, candles.get(0).getTradePrice(), bollingerBand[1]));
            }

            // 요청 제한을 대비하여 300ms 대기
            delayMethod(150);
        }

        Collections.sort(evaluations);
        int inquirySize = inquiries.size() - 1;
        List<CoinEvaluation> topCoins = evaluations.stream()
                .limit(3-inquirySize)
                .toList();

        for (CoinEvaluation coin : topCoins) {
            String bidPrice = String.valueOf((long) (krwBalance * bidPercentage));
            orderCoin(new CoinOrderRequest(coin.getMarket(), "bid", null, bidPrice, "price"));
        }
    }

    @Scheduled(cron = "0 0 */1 * * *")
    public void evaluateHoldingsForSell() {
        int period = 20;
        int unit = 240;

        List<CoinInquiryResponse> inquiries = getAccountInquiry();
        for (CoinInquiryResponse inquiry : inquiries) {
            List<CandleResponse> candles = getCandles(unit, inquiry.getCurrency(), period);
            double rsi = technicalIndicator.calculateRSI(candles, 14);
            double[] bollingerBands = technicalIndicator.calculateBollingerBand(candles, period);

            double currentMarketPrice = candles.get(0).getTradePrice();
            double avgBuyPrice = inquiry.getAvgBuyPrice();
            double lossPercentage = (currentMarketPrice - avgBuyPrice) / avgBuyPrice * 100;

            // 매도 조건
            if (rsi > 70 || candles.get(0).getTradePrice() > bollingerBands[0] || lossPercentage <= -5) {
                String market = inquiry.getUnitCurrency() + "-" + inquiry.getCurrency();
                orderCoin(new CoinOrderRequest(market, "ask", String.valueOf(inquiry.getBalance()), null, "market"));
            }
        }
    }

    private static void delayMethod(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "요청 대기 중 오류가 발생했습니다.");
        }
    }

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
        String token = jwtTokenUtil.getAuthenticationToken(upbitProperties, queryHash);

        // webclient 안됨. httpclient로 대체
        HttpEntity entity;
        try {
            HttpClient client = HttpClientBuilder.create().build();
            HttpPost request = new HttpPost(upbitProperties.getServerUrl() + "/v1/orders");
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

        discordService.sendDiscordAlertLog("매매 알림",
                coinOrderRequest.getMarket(), coinOrderRequest.getPrice(),
                coinOrderRequest.getVolume(), coinOrderRequest.getSide());
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
}
