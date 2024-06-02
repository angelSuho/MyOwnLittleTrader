package com.trader.coin.binance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trader.coin.common.Tech.service.TechnicalIndicator;
import com.trader.coin.common.infrastructure.ProfitPercentageRepository;
import com.trader.coin.common.infrastructure.alert.discord.DiscordService;
import com.trader.coin.common.infrastructure.config.APIProperties;
import com.trader.coin.crypto.infrastructure.jwt.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public List<String> findMarkets() {
        WebClient client = WebClient.builder()
                .baseUrl(api.getBinance().getServerUrl()) // URL은 여기에 설정
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + api.getBinance().getSecretKey())
                .build();

        return client.get()
                .uri("/fapi/v1/ping")
                .retrieve()
                .bodyToMono(String.class)
                .map(responseBody -> {
                    try {
                        List<Map<String, String>> responseList = objectMapper.readValue(responseBody, new TypeReference<>() {
                        });
                        return responseList.stream().map(map -> map.get("market")).collect(Collectors.toList());
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("응답 데이터 변환에 실패했습니다.", e);
                    }
                }).block();
    }

}