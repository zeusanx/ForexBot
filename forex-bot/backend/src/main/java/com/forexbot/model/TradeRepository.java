package com.forexbot.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findAllByOrderByOpenTimeDesc();
    List<Trade> findByStatus(Trade.TradeStatus status);
    List<Trade> findByCurrencyPair(String pair);

    @Query("SELECT SUM(t.profitLoss) FROM Trade t WHERE t.status = 'CLOSED'")
    Double sumTotalProfitLoss();
}
