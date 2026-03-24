package com.forexbot.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CandlestickRepository extends JpaRepository<Candlestick, Long> {
    List<Candlestick> findByCurrencyPairOrderByTimestampDesc(String currencyPair);

    @Query("SELECT c FROM Candlestick c WHERE c.currencyPair = :pair ORDER BY c.timestamp DESC")
    List<Candlestick> findLatestByCurrencyPair(String pair, org.springframework.data.domain.Pageable pageable);
}
