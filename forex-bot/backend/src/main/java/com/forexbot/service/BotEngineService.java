package com.forexbot.service;

import com.forexbot.broker.MockBrokerService;
import com.forexbot.model.BotState;
import com.forexbot.model.BotStateRepository;
import com.forexbot.model.Candlestick;
import com.forexbot.strategy.EmaStrategy;
import com.forexbot.strategy.EmaStrategy.Signal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BotEngineService {

    private static final Logger log = LoggerFactory.getLogger(BotEngineService.class);
    private static final String STATE_KEY = "MAIN";

    private final MarketDataService    marketData;
    private final EmaStrategy          ema;
    private final MockBrokerService    broker;
    private final BotStateRepository   stateRepo;

    @Value("${broker.initial.balance:10000.00}")
    private double initialBalance;

    @Value("${strategy.ema.long-period:21}")
    private int warmUp;

    public BotEngineService(MarketDataService m, EmaStrategy e,
                            MockBrokerService b, BotStateRepository r) {
        marketData = m; ema = e; broker = b; stateRepo = r;
    }

    // ── Start / Stop ───────────────────────────────────────────────────────────

    @Transactional
    public BotState startBot(String pair, double targetProfit, double maxLoss) {
        BotState s = getOrCreate();
        if (s.isRunning()) { log.info("Already running"); return s; }

        String p = (pair != null && !pair.isBlank()) ? pair : marketData.getDefaultPair();

        broker.initialize(initialBalance);
        broker.setSlTp(targetProfit, maxLoss);

        s.setRunning(true);
        s.setActivePair(p);
        s.setWalletBalance(initialBalance);
        s.setLastSignal("NONE");
        s.setUpdatedAt(LocalDateTime.now());

        // Seed warm-up candles if needed
        int need = ema.getLongPeriod() + 10;
        if (marketData.getLatestCandles(p, need).size() < need)
            marketData.seedHistoricalData(p, need + 5);

        log.info("Bot STARTED: pair={} TP=${} SL=${}", p, targetProfit, maxLoss);
        return stateRepo.save(s);
    }

    @Transactional
    public BotState stopBot() {
        BotState s = getOrCreate();
        if (!s.isRunning()) { log.info("Already stopped"); return s; }

        if (s.getActivePair() != null) {
            List<Candlestick> c = marketData.getLatestCandles(s.getActivePair(), 1);
            if (!c.isEmpty())
                broker.closeOpenPositions(s.getActivePair(), c.get(0).getClose());
        }

        s.setRunning(false);
        s.setWalletBalance(broker.getWalletBalance());
        s.setUpdatedAt(LocalDateTime.now());
        log.info("Bot STOPPED. Balance: {}", broker.getWalletBalance());
        return stateRepo.save(s);
    }

    // ── Trading cycle ──────────────────────────────────────────────────────────

    @Transactional
    public void executeTradingCycle() {
        BotState s = getOrCreate();
        if (!s.isRunning()) return;

        String pair = s.getActivePair();
        if (pair == null) return;

        try {
            marketData.fetchAndStoreLatestCandle(pair);

            int need = ema.getLongPeriod() + 10;
            List<Candlestick> candles = marketData.getLatestCandles(pair, need);
            if (candles.size() < ema.getLongPeriod() + 2) {
                log.debug("Warming up {}/{}", candles.size(), ema.getLongPeriod());
                return;
            }

            boolean hasBuy  = broker.hasOpenBuy(pair);
            boolean hasSell = broker.hasOpenSell(pair);
            Signal signal   = ema.evaluateSignal(candles, hasBuy, hasSell);
            double[] emaVals = ema.getCurrentEmaValues(candles);
            double close     = candles.get(candles.size() - 1).getClose();

            broker.processSignal(signal, pair, close);

            s.setCurrentEmaShort(emaVals[0]);
            s.setCurrentEmaLong(emaVals[1]);
            s.setLastSignal(signal.name());
            s.setWalletBalance(broker.getWalletBalance());
            s.setUpdatedAt(LocalDateTime.now());
            stateRepo.save(s);

            log.info("[CYCLE] sig={} EMA9={} EMA21={} close={} BUY={} SELL={} bal={}",
                    signal,
                    String.format("%.5f", emaVals[0]),
                    String.format("%.5f", emaVals[1]),
                    String.format("%.5f", close),
                    hasBuy, hasSell,
                    String.format("%.2f", broker.getWalletBalance()));

        } catch (Exception e) {
            log.error("Cycle error: {}", e.getMessage(), e);
        }
    }

    public BotState getBotState() { return getOrCreate(); }

    private BotState getOrCreate() {
        return stateRepo.findByStateKey(STATE_KEY).orElseGet(() -> {
            BotState s = BotState.builder()
                    .stateKey(STATE_KEY).running(false)
                    .walletBalance(initialBalance)
                    .currentEmaShort(0.0).currentEmaLong(0.0)
                    .lastSignal("NONE")
                    .activePair(marketData.getDefaultPair())
                    .updatedAt(LocalDateTime.now()).build();
            return stateRepo.save(s);
        });
    }
}
