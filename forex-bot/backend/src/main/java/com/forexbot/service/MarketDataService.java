package com.forexbot.service;

import com.forexbot.model.Candlestick;
import com.forexbot.model.CandlestickRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Market Data Service.
 *
 * PRIMARY: Twelvedata /time_series for real EUR/USD OHLCV candles.
 * FALLBACK: Realistic random-walk mock using CURRENT real price range (1.15xx).
 *
 * FIX: Mock data now starts at actual current EUR/USD price level (~1.1590)
 * so there are no wild jumps. The lastMockPrice is initialised from the
 * first successful Twelvedata fetch, or falls back to the hard-coded default
 * which is set to the approximate real price.
 */
@Service
public class MarketDataService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);

    private final CandlestickRepository candlestickRepository;
    private final WebClient webClient;

    @Value("${forex.api.key:demo}")
    private String apiKey;

    @Value("${forex.default.pair:EUR/USD}")
    private String defaultPair;

    private final Random random = new Random();

    // ── IMPORTANT: Updated to current real EUR/USD price range (March 2026) ──
    // When you add a Twelvedata key this value will be overridden by real data
    private double lastMockPrice = 1.1590;
    private boolean priceCalibrated = false;

    public MarketDataService(CandlestickRepository repo, WebClient.Builder builder) {
        this.candlestickRepository = repo;
        this.webClient = builder.build();
    }

    // ── Public ─────────────────────────────────────────────────────────────────

    public Candlestick fetchAndStoreLatestCandle(String pair) {
        boolean demo = apiKey == null
                || apiKey.equalsIgnoreCase("demo")
                || apiKey.equalsIgnoreCase("YOUR_TWELVEDATA_KEY_HERE");
        if (!demo) {
            try {
                Candlestick real = fetchFromTwelvedata(pair);
                if (real != null) {
                    // Calibrate mock price to real market level
                    lastMockPrice = real.getClose();
                    priceCalibrated = true;
                    return real;
                }
            } catch (Exception e) {
                log.warn("Twelvedata fetch failed: {} — using mock", e.getMessage());
            }
        }
        return generateMockCandle(pair);
    }

    /**
     * Seed historical data. We generate smooth candles starting from the
     * target price so there are no spikes in the chart.
     */
    public void seedHistoricalData(String pair, int count) {
        log.info("Seeding {} historical candles for {}", count, pair);
        // If we have a real price already from DB, start from that
        List<Candlestick> existing = candlestickRepository
                .findLatestByCurrencyPair(pair, PageRequest.of(0, 1));
        if (!existing.isEmpty()) {
            lastMockPrice = existing.get(0).getClose();
        }
        for (int i = 0; i < count; i++) {
            generateMockCandle(pair);
        }
    }

    public List<Candlestick> getLatestCandles(String pair, int limit) {
        List<Candlestick> candles = candlestickRepository
                .findLatestByCurrencyPair(pair, PageRequest.of(0, limit));
        Collections.reverse(candles);
        return candles;
    }

    public List<Candlestick> getAllCandles(String pair) {
        return candlestickRepository.findByCurrencyPairOrderByTimestampDesc(pair);
    }

    public String getDefaultPair() { return defaultPair; }

    // ── Twelvedata ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Candlestick fetchFromTwelvedata(String pair) {
        Map<String, Object> response = webClient.get()
                .uri(b -> b.scheme("https").host("api.twelvedata.com")
                        .path("/time_series")
                        .queryParam("symbol", pair)
                        .queryParam("interval", "5min")
                        .queryParam("outputsize", "2")
                        .queryParam("apikey", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || response.containsKey("code") || !response.containsKey("values")) {
            log.warn("Twelvedata bad response: {}", response != null ? response.get("message") : "null");
            return null;
        }

        List<Map<String, String>> values = (List<Map<String, String>>) response.get("values");
        if (values == null || values.isEmpty()) return null;

        Map<String, String> c = values.get(0);
        double open  = parse(c.get("open"));
        double high  = parse(c.get("high"));
        double low   = parse(c.get("low"));
        double close = parse(c.get("close"));
        if (close <= 0) return null;

        long vol = 1000L;
        try { if (c.containsKey("volume")) vol = Long.parseLong(c.get("volume")); } catch (Exception ignored) {}

        Candlestick candle = Candlestick.builder()
                .currencyPair(pair).open(r5(open)).high(r5(high)).low(r5(low)).close(r5(close))
                .volume(vol).timestamp(LocalDateTime.now()).build();

        log.info("Twelvedata: O={} H={} L={} C={}", r5(open), r5(high), r5(low), r5(close));
        return candlestickRepository.save(candle);
    }

    // ── Mock generator ─────────────────────────────────────────────────────────
    // Realistic 5-minute EUR/USD simulation
    // Volatility: ~3-5 pips per candle (5m EUR/USD typical range)
    // Mean reversion toward 1.1590 (current real price as of March 2026)

    private static final double REAL_MEAN = 1.1590;   // ← update this if needed
    private static final double VOLATILITY = 0.0004;  // 4 pips per candle

    private Candlestick generateMockCandle(String pair) {
        // Geometric Brownian Motion style with mean reversion
        double shock    = random.nextGaussian() * VOLATILITY;
        double revert   = (REAL_MEAN - lastMockPrice) * 0.03;
        double newClose = lastMockPrice + shock + revert;

        // Hard bounds: EUR/USD realistic range
        newClose = Math.max(1.05, Math.min(1.35, newClose));

        double open  = r5(lastMockPrice);
        double close = r5(newClose);
        double range = Math.abs(random.nextGaussian() * VOLATILITY * 0.6);
        double high  = r5(Math.max(open, close) + range);
        double low   = r5(Math.min(open, close) - range);
        long   vol   = 800L + (long)(random.nextDouble() * 3000);

        lastMockPrice = newClose;

        Candlestick c = Candlestick.builder()
                .currencyPair(pair).open(open).high(high).low(low).close(close)
                .volume(vol).timestamp(LocalDateTime.now()).build();
        return candlestickRepository.save(c);
    }

    private double parse(String s) {
        try { return s != null ? Double.parseDouble(s) : 0; } catch (Exception e) { return 0; }
    }

    private double r5(double v) { return Math.round(v * 100000.0) / 100000.0; }
}
