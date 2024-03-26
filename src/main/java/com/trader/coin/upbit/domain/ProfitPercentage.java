package com.trader.coin.upbit.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfitPercentage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String market;
    private double profitAndLossPercentage;

    public ProfitPercentage(String market, double profitAndLossPercentage) {
        this.market = market;
        this.profitAndLossPercentage = profitAndLossPercentage;
    }

    public void updateProfitPercentage(double profitPercentage) {
        this.profitAndLossPercentage = profitPercentage;
    }
}
