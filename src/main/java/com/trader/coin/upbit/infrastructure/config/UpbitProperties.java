package com.trader.coin.upbit.infrastructure.config;

import com.trader.coin.common.infrastructure.config.ConfigProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "upbit")
@Getter @Setter
public class UpbitProperties implements ConfigProperties {
    private String accessKey;
    private String secretKey;
    private String serverUrl;
}
