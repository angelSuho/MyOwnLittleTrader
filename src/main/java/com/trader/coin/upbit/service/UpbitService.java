package com.trader.coin.upbit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.trader.coin.common.infrastructure.alert.discord.DiscordService;
import com.trader.coin.common.infrastructure.config.exception.BaseException;
import com.trader.coin.common.infrastructure.config.exception.ErrorCode;
import com.trader.coin.crypto.infrastructure.jwt.JwtTokenUtil;
import com.trader.coin.upbit.domain.dto.AccountInquiryResponse;
import com.trader.coin.upbit.domain.dto.UpbitOrderRequest;
import com.trader.coin.upbit.infrastructure.config.UpbitProperties;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UpbitService {
    private final JwtTokenUtil jwtTokenUtil;
    private final DiscordService discordService;

    private final UpbitProperties upbitProperties;

    private final ObjectMapper objectMapper;

    public List<AccountInquiryResponse> getAccountInquiry() {
        String token = jwtTokenUtil.getAuthenticationToken(upbitProperties);
        WebClient client = WebClient.create(upbitProperties.getServerUrl());

        String responseBody = client.get()
                .uri("/v1/accounts")
                .header("Content-Type", "application/json")
                .header("Authorization", token)
                .retrieve() // 요청을 실행하고 결과를 검색
                .bodyToMono(String.class) // 응답 본문을 String으로 변환
                .block(); // 구독을 시작하고 결과를 기다림 (블로킹 호출)

        List<AccountInquiryResponse> responseList;
        try {
            responseList = objectMapper.readValue(responseBody, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "응답 데이터 변환에 실패했습니다.");
        }
        return responseList;
    }

    public void orderCoin(UpbitOrderRequest upbitOrderRequest) {
        HashMap<String, String> params = getParams(upbitOrderRequest);
        List<String> queryElements = new ArrayList<>();
        for(Map.Entry<String, String> entity : params.entrySet()) {
            queryElements.add(entity.getKey() + "=" + entity.getValue());
        }
        String queryString = String.join("&", queryElements.toArray(new String[0]));

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-512");
            md.update(queryString.getBytes("UTF-8"));
        } catch (Exception e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "해시 생성에 실패했습니다.");
        }

        String queryHash = String.format("%0128x", new BigInteger(1, md.digest()));
        String token = jwtTokenUtil.getAuthenticationToken(upbitProperties, queryHash);

        HttpEntity entity;
        try {
            HttpClient client = HttpClientBuilder.create().build();
            HttpPost request = new HttpPost(upbitProperties.getServerUrl() + "/v1/orders");
            request.setHeader("Content-Type", "application/json");
            request.addHeader("Authorization", token);
            request.setEntity(new StringEntity(new Gson().toJson(params)));

            HttpResponse response = client.execute(request);
            entity = response.getEntity();

            System.out.println(EntityUtils.toString(entity, "UTF-8"));
        } catch (IOException e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "주문 요청에 실패했습니다.");
        }

        discordService.sendDiscordAlertLog("매매 알림",
                upbitOrderRequest.getMarket(), upbitOrderRequest.getPrice(),
                upbitOrderRequest.getVolume(), upbitOrderRequest.getSide());
    }

    private static HashMap<String, String> getParams(UpbitOrderRequest upbitOrderRequest) {
        HashMap<String, String> params = new HashMap<>();
        if (upbitOrderRequest.getVolume() == null) {
            params.put("market", upbitOrderRequest.getMarket());
            params.put("side", upbitOrderRequest.getSide());
            params.put("price", upbitOrderRequest.getPrice());
            params.put("ord_type", upbitOrderRequest.getOrd_type());
        } else {
            params.put("market", upbitOrderRequest.getMarket());
            params.put("side", upbitOrderRequest.getSide());
            params.put("volume", upbitOrderRequest.getVolume());
            params.put("price", upbitOrderRequest.getPrice());
            params.put("ord_type", upbitOrderRequest.getOrd_type());
        }
        return params;
    }
}
