package com.trader.coin.upbit.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "api")
@Getter @Setter
public class APIProperties {
    private api upbit;
    private api binance;

    // 매수하지 않을 코인 리스트
    private final List<String> COIN_NOT_BUY = List.of(
            "KRW-TRX", "KRW-XRP", "KRW-WAXP", "KRW-WAVES", "KRW-MTL"
    );
    // 매도하지 않을 코인 리스트
    private final List<String> COIN_NOT_SELL = List.of(
            "KRW-BTC", "KRW-ETH"
    );

    private final String crypto_market = "upbit";
    private final String UNIT = "240";  // days or minutes(1, 3, 5, 15, 30, 60, 240)
    private final int PERIOD = 20;
    private final double BID_PERCENTAGE = 0.25;
    private final int CANDLE_COUNT = 200;
    private final int RSI_PERIOD = 14;

    private final int RSI_BUYING_CONDITION = 35;
    private final int RSI_SELLING_CONDITION = 75;

    private final int AFFORD_LOSS_VALUE = 3;
    private final int MAX_LOSS_VALUE = 6;
    private final int STOP_LOSS_LINE = -3;

    private final int NUMBER_OF_BUY = 3;

    @Getter @Setter
    public static class api {
        protected String accessKey;
        protected String secretKey;
        protected String serverUrl;
    }
}
