package com.trader.coin.upbit.presentation;

import com.trader.coin.common.infrastructure.config.exception.BaseException;
import com.trader.coin.common.infrastructure.config.exception.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class CoinOrderRequest {
    @NotBlank(message = "주문할 코인 이름을 입력해주세요. ex) KRW-BTC")
    private String market;
    @NotBlank(message = "주문 종류를 입력해주세요. ex) ask 또는 bid")
    private String side;
    private String volume;
    private String price;
    @NotBlank(message = "주문 타입을 입력해주세요. ex) limit, price, market")
    private String ord_type;

    public CoinOrderRequest(String market, String side, String volume, String price, String ordType) {
        this.market = market;
        checkSide(side);
        this.side = side;
        this.volume = volume;
        this.price = price;
        checkOrdType(ordType);
        this.ord_type = ordType;
    }

    private void checkSide(String side) {
        if (!side.equals("ask") && !side.equals("bid")) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "side 값은 ask 또는 bid만 가능합니다.");
        }
    }

    private void checkOrdType(String ordType) {
        if (!ordType.equals("limit") && !ordType.equals("price") && !ordType.equals("market")) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "ord_type 값은 limit, price, market만 가능합니다.");
        }
    }
}
