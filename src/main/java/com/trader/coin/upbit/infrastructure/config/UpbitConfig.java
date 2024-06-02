package com.trader.coin.upbit.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(APIProperties.class)
public class UpbitConfig {}
