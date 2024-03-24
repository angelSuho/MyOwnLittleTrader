package com.trader.coin.common.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "webhook")
@Getter @Setter
public class WebhookProperties {
    private Url discordAlert;
    private Url upbit;

    @Getter @Setter
    public static class Url {
        private String url;
    }
}
