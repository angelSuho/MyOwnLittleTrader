package com.trader.coin.crypto.infrastructure.config;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import lombok.RequiredArgsConstructor;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableEncryptableProperties
public class JasyptConfig {

    private final CryptoProperties cryptoProperties;

    @Bean
    public StringEncryptor jasyptStringEncryptor() {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        encryptor.setAlgorithm(cryptoProperties.getAlgorithm());
        encryptor.setPoolSize(cryptoProperties.getPoolSize());
        encryptor.setStringOutputType(cryptoProperties.getStringOutputType());
        encryptor.setKeyObtentionIterations(cryptoProperties.getKeyObtentionIterations());
        encryptor.setPassword(cryptoProperties.getPassword());
        return encryptor;
    }
}
