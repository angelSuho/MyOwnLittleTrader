package com.trader.coin.upbit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.trader.coin.common.Tech.service.TechnicalIndicator;
import com.trader.coin.common.domain.MATrendDirection;
import com.trader.coin.common.infrastructure.alert.discord.DiscordService;
import com.trader.coin.common.infrastructure.config.exception.BaseException;
import com.trader.coin.common.infrastructure.config.exception.ErrorCode;
import com.trader.coin.crypto.infrastructure.jwt.JwtTokenUtil;
import com.trader.coin.upbit.domain.CoinEvaluation;
import com.trader.coin.upbit.domain.ProfitPercentage;
import com.trader.coin.upbit.infrastructure.ProfitPercentageRepository;
import com.trader.coin.upbit.infrastructure.config.UpbitProperties;
import com.trader.coin.upbit.presentation.dto.CoinInquiryResponse;
import com.trader.coin.upbit.presentation.dto.CoinOrderRequest;
import com.trader.coin.upbit.service.dto.CandleResponse;
import com.trader.coin.upbit.service.dto.TickerResponse;
import jakarta.persistence.EntityManager;
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
import java.util.stream.Collectors;

import static com.trader.coin.common.domain.MATrendDirection.*;
import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpbitService {
    private final EntityManager em;
    private final ProfitPercentageRepository profitPercentageRepository;

    private final ObjectMapper objectMapper;
    private final UpbitProperties upbitProperties;

    private final JwtTokenUtil jwtTokenUtil;
    private final DiscordService discordService;
    private final TechnicalIndicator technicalIndicator;

    // 구매하지 않을 코인 리스트
    private static final List<String> COIN_NOT_BUY = List.of(
            "KRW-TRX", "KRW-XRP"
    );

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

    public List<CandleResponse> getCandles(String unit, String market, int count) {
        WebClient client = WebClient.create(upbitProperties.getServerUrl());
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

    public void waitAndSeeOrderCoin() {
        // unit == null ? days : 240
        String unit = "240";
        double bidPercentage = 0.25;
        List<CoinInquiryResponse> inquiries = getAccountInquiry();
        log.warn("{} 보유 코인 매수 조건 평가 시작", getLocalDateTimeNow());

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

            int period = 20;
            int count = 200;
            List<CandleResponse> candles = getCandles(unit, market, count);
            if (candles.size() < count) {
                continue;
            }
            double rsi = technicalIndicator.calculateUpbitRSI(candles, 14);
            long[] bollingerBand = technicalIndicator.calculateBollingerBand(candles, period);
            
            // 매수 조건
            // 볼린저 밴드 하단 범위 설정. 하단 돌파부터 근접
            double lowerBand = bollingerBand[1];
            double lowerBandNear = lowerBand * 1.05; // 볼린저 밴드 하단의 5% 위
            boolean isBandTrue = candles.get(0).getTradePrice() <= lowerBandNear && candles.get(0).getTradePrice() >= lowerBand;
            MATrendDirection goldenCross = technicalIndicator.isGoldenCross(candles);

            if (rsi < 40 && isBandTrue && !COIN_NOT_BUY.contains(market) || goldenCross == GOLDEN_CROSS) {
                evaluations.add(new CoinEvaluation(market, rsi, candles.get(0).getTradePrice(), bollingerBand[1]));
            }

            delayMethod(150);
        }

        if (evaluations.isEmpty()) {
            log.error("매수할 코인이 없습니다.");
            return;
        }
        // 조건 부합 코인 출력
        evaluations.forEach(evaluation -> log.info("market: {}, RSI: {}, 가격: {}, 볼린저밴드 하단: {}", evaluation.getMarket(), evaluation.getRsi(), evaluation.getTradePrice(), evaluation.getLowerBollingerBand()));

        Random random = new Random();
        List<Integer> randomIndexes = random.ints(0, evaluations.size())
                .distinct()
                .limit(Math.min(3, evaluations.size()))
                .boxed()
                .toList();

        // 랜덤 인덱스에 해당하는 CoinEvaluation 객체를 추출
        List<CoinEvaluation> randomPicks = randomIndexes.stream()
                .map(evaluations::get)
                .toList();

        if (inquiries.stream().anyMatch(inquiry -> inquiry.getCurrency().equals("KRW") && inquiry.getBalance() <= 5_000)) {
            log.error("KRW 잔고가 5000원 이하이므로 매수하지 않습니다.");
            return;
        }

        for (CoinEvaluation coin : randomPicks) {
            String bidPrice = String.valueOf((long) (krwBalance * bidPercentage));
            orderCoin(new CoinOrderRequest(coin.getMarket(), "bid", null, bidPrice, "price"));
            log.info("market: {}, 가격: {} 매수", coin.getMarket(), bidPrice);
        }

        arrangeProfit(inquiries);
    }

    @Transactional
    public void evaluateHoldingsForSell() {
        delayMethod(2000);
        log.warn("{} 보유 코인 매도 조건 평가 시작", getLocalDateTimeNow());
        List<CoinInquiryResponse> inquiries = getAccountInquiry();
        List<ProfitPercentage> profitPercentages = arrangeProfit(inquiries);
        if (inquiries.size() <= 1) {
            log.info("보유 코인이 없어 매도하지 않습니다.");
            return;
        }

        int period = 20;
        // unit == null ? 240 : days
        String unit = "240";

        for (CoinInquiryResponse inquiry : inquiries) {
            if (inquiry.getCurrency().equals("KRW")) {
                continue;
            }
            String market = inquiry.getUnitCurrency() + "-" + inquiry.getCurrency();

            List<CandleResponse> candles = getCandles(unit, market, 200);
            double rsi = technicalIndicator.calculateUpbitRSI(candles, 14);
            long[] bollingerBands = technicalIndicator.calculateBollingerBand(candles, period);

            double currentMarketPrice = candles.get(0).getTradePrice();
            double avgBuyPrice = inquiry.getAvgBuyPrice();
            ProfitPercentage profitPercentage = profitPercentages.stream().filter(profit -> profit.getMarket().equals(market))
                    .findFirst().orElseThrow(() -> new BaseException(ErrorCode.NOT_FOUND, "해당 코인의 손익률을 찾을 수 없습니다."));
            double profitAndLossPercentage = (currentMarketPrice - avgBuyPrice) / avgBuyPrice * 100;

            // 최대 수익률에서 5% 빠졌는지 확인합니다.
            boolean hasDroppedFromMaxProfit = profitAndLossPercentage < profitPercentage.getProfitAndLossPercentage() - 5;
            // 현재 손익률이 -3% 이하인지 확인합니다.
            boolean isLossGreaterThan3Percent = profitAndLossPercentage <= -3;

            // 매도 조건
            if ((rsi > 75 && candles.get(0).getTradePrice() > bollingerBands[0]) || isLossGreaterThan3Percent || hasDroppedFromMaxProfit) {
                orderCoin(new CoinOrderRequest(market, "ask", String.valueOf(inquiry.getBalance()), null, "market"));
                profitPercentageRepository.deleteByMarket(market);
                discordService.sendDiscordAlertLog(market, String.valueOf(candles.get(0).getTradePrice()),
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
                log.info("매도 조건이 충족되지 않아 매도하지 않습니다.");
            }
        }
    }

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
        log.info("현재 {} 손익률 계산 완료", now);
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

        discordService.sendDiscordAlertLog(
                coinOrderRequest.getMarket(), coinOrderRequest.getPrice(),
                coinOrderRequest.getVolume(), coinOrderRequest.getSide());
    }

    public void delayMethod(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "요청 대기 중 오류가 발생했습니다.");
        }
    }

    @Transactional
    private List<ProfitPercentage> arrangeProfit(List<CoinInquiryResponse> inquiries) {
        List<ProfitPercentage> percentages = profitPercentageRepository.findAll();
        List<String> markets = inquiries.stream()
                .filter(inquiry -> !inquiry.getCurrency().equals("KRW"))
                .map(inquiry -> inquiry.getUnitCurrency() + "-" + inquiry.getCurrency())
                .toList();

        // 삭제할 ProfitPercentage 객체를 찾습니다.
        List<ProfitPercentage> toDelete = percentages.stream()
                .filter(profitPercentage -> !markets.contains(profitPercentage.getMarket()))
                .toList();

        // 삭제할 ProfitPercentage 객체를 삭제합니다.
        toDelete.forEach(profitPercentageRepository::delete);

        // 삭제 후 남아 있는 ProfitPercentage 객체를 반환합니다.
        percentages.removeAll(toDelete);
        return percentages;
    }

    private TickerResponse getTicker(String market) {
        WebClient client = WebClient.create(upbitProperties.getServerUrl());
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

    private String getLocalDateTimeNow() {
        return DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분 ss초").format(LocalDateTime.now());
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
