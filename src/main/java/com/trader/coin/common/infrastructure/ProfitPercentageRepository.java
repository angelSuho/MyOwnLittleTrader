package com.trader.coin.common.infrastructure;

import com.trader.coin.upbit.domain.ProfitPercentage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfitPercentageRepository extends JpaRepository<ProfitPercentage, Long> {
    Optional<ProfitPercentage> findByMarket(String market);
    void deleteByMarket(String market);
}
