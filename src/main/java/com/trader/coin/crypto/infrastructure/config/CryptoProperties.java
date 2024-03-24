package com.trader.coin.crypto.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "crypto.jasypt.encryptor")
@Getter @Setter
public class CryptoProperties {
    private String algorithm;
    private int poolSize;
    private String stringOutputType;
    private int keyObtentionIterations;
    private String password;
}
