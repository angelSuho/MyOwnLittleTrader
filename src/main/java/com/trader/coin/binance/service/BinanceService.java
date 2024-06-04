package com.trader.coin.binance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trader.coin.binance.service.dto.MarketResponse;
import com.trader.coin.common.Tech.service.TechnicalIndicator;
import com.trader.coin.common.infrastructure.ProfitPercentageRepository;
import com.trader.coin.common.infrastructure.alert.discord.DiscordService;
import com.trader.coin.common.infrastructure.config.APIProperties;
import com.trader.coin.common.infrastructure.config.exception.BaseException;
import com.trader.coin.common.infrastructure.config.exception.ErrorCode;
import com.trader.coin.crypto.infrastructure.jwt.JwtTokenUtil;
import com.trader.coin.upbit.service.dto.CandleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

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

        System.out.println(responseBody);
        List<CandleResponse> responseList;
        try {
            responseList = objectMapper.readValue(responseBody, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "응답 데이터 변환에 실패했습니다.");
        }

        return responseList;
    }





}