package com.forexbot.broker;

import com.forexbot.model.Trade;
import com.forexbot.model.TradeRepository;
import com.forexbot.strategy.EmaStrategy.Signal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Mock Broker with SL / TP management.
 *
 * Each trade carries a stopLossPips  and takeProfitPips field.
 * On every cycle (checkSlTp call), open positions are evaluated
 * against current price — if SL or TP is hit the trade closes.
 *
 * SL and TP are expressed in account currency ($), not pips,
 * so the user can set "target $20, risk $5" directly.
 *
 * Position flow:
 *   Signal.BUY        → open BUY  (if flat)
 *   Signal.SELL       → open SELL (if flat)
 *   Signal.CLOSE_BUY  → close BUY  position
 *   Signal.CLOSE_SELL → close SELL position
 *   Signal.HOLD       → check SL/TP only
 */
@Service
@Transactional
public class MockBrokerService {

    private static final Logger log = LoggerFactory.getLogger(MockBrokerService.class);

    private final TradeRepository tradeRepository;

    @Value("${broker.initial.balance:10000.00}")
    private double initialBalance;

    @Value("${broker.trade.lot-size:1000}")
    private int defaultLotSize;

    @Value("${broker.spread:0.0002}")
    private double spread;

    private double walletBalance;
    private boolean initialized = false;

    // SL / TP in dollar amounts — set by user before starting
    private double targetProfit = 20.0;   // close trade when profit >= $20
    private double maxLoss      = 7.0;    // close trade when loss >= $7

    public MockBrokerService(TradeRepository repo) { this.tradeRepository = repo; }

    // ── Init ───────────────────────────────────────────────────────────────────

    public void initialize(double balance) {
        this.walletBalance = balance;
        this.initialized   = true;
        log.info("Broker initialized: balance=${}", balance);
    }

    public void setSlTp(double targetProfit, double maxLoss) {
        this.targetProfit = targetProfit;
        this.maxLoss      = maxLoss;
        log.info("SL/TP set: target=${} maxLoss=${}", targetProfit, maxLoss);
    }

    public double getTargetProfit() { return targetProfit; }
    public double getMaxLoss()      { return maxLoss; }

    // ── Main signal processor ──────────────────────────────────────────────────

    public Optional<Trade> processSignal(Signal signal, String pair, double price) {
        if (!initialized) { walletBalance = initialBalance; initialized = true; }

        // First: check if any open trade hits SL or TP
        checkSlTp(pair, price);

        switch (signal) {
            case BUY:         return openTrade(Trade.TradeType.BUY,  pair, price);
            case SELL:        return openTrade(Trade.TradeType.SELL, pair, price);
            case CLOSE_BUY:   closeByType(pair, Trade.TradeType.BUY,  price, "EMA_EXIT"); return Optional.empty();
            case CLOSE_SELL:  closeByType(pair, Trade.TradeType.SELL, price, "EMA_EXIT"); return Optional.empty();
            default:          return Optional.empty();
        }
    }

    // ── SL / TP check — called every cycle ────────────────────────────────────

    public void checkSlTp(String pair, double price) {
        for (Trade t : tradeRepository.findByStatus(Trade.TradeStatus.OPEN)) {
            if (!t.getCurrencyPair().equals(pair)) continue;
            double pnl = unrealisedPnl(t, price);
            if (pnl >= targetProfit) {
                log.info("TP HIT for trade #{} pnl={}", t.getId(), pnl);
                closeTrade(t, price, "TP_HIT");
            } else if (pnl <= -maxLoss) {
                log.info("SL HIT for trade #{} pnl={}", t.getId(), pnl);
                closeTrade(t, price, "SL_HIT");
            }
        }
    }

    private double unrealisedPnl(Trade t, double price) {
        double mid = t.getType() == Trade.TradeType.BUY
                ? price - spread / 2
                : price + spread / 2;
        double diff = t.getType() == Trade.TradeType.BUY
                ? mid - t.getEntryPrice()
                : t.getEntryPrice() - mid;
        return diff * t.getLotSize();
    }

    // ── Open position ──────────────────────────────────────────────────────────

    private Optional<Trade> openTrade(Trade.TradeType type, String pair, double price) {
        // Skip if already holding same direction
        boolean exists = tradeRepository.findByStatus(Trade.TradeStatus.OPEN).stream()
                .anyMatch(t -> t.getCurrencyPair().equals(pair) && t.getType() == type);
        if (exists) { log.debug("Already in {} — skip", type); return Optional.empty(); }

        double entry = type == Trade.TradeType.BUY
                ? price + spread / 2
                : price - spread / 2;

        double margin = entry * defaultLotSize * 0.01;
        if (walletBalance < margin) {
            log.warn("Insufficient margin: balance={} need={}", walletBalance, margin);
            return Optional.empty();
        }

        String sig = type == Trade.TradeType.BUY ? "EMA_GOLDEN_CROSS" : "EMA_DEATH_CROSS";
        Trade trade = Trade.builder()
                .currencyPair(pair).type(type).status(Trade.TradeStatus.OPEN)
                .entryPrice(entry).lotSize(defaultLotSize)
                .openTime(LocalDateTime.now()).signal(sig).profitLoss(0.0)
                .build();

        Trade saved = tradeRepository.save(trade);
        log.info("OPENED {} {} @ {} | SL=${} TP=${}",
                type, pair, String.format("%.5f", entry), maxLoss, targetProfit);
        return Optional.of(saved);
    }

    // ── Close helpers ──────────────────────────────────────────────────────────

    private void closeByType(String pair, Trade.TradeType type, double price, String reason) {
        tradeRepository.findByStatus(Trade.TradeStatus.OPEN).stream()
                .filter(t -> t.getCurrencyPair().equals(pair) && t.getType() == type)
                .forEach(t -> closeTrade(t, price, reason));
    }

    public void closeOpenPositions(String pair, double price) {
        tradeRepository.findByStatus(Trade.TradeStatus.OPEN).stream()
                .filter(t -> t.getCurrencyPair().equals(pair))
                .forEach(t -> closeTrade(t, price, "BOT_STOPPED"));
    }

    private void closeTrade(Trade t, double price, String reason) {
        double exit = t.getType() == Trade.TradeType.BUY
                ? price - spread / 2
                : price + spread / 2;
        double diff = t.getType() == Trade.TradeType.BUY
                ? exit - t.getEntryPrice()
                : t.getEntryPrice() - exit;
        double pnl = diff * t.getLotSize();

        t.setExitPrice(exit);
        t.setProfitLoss(pnl);
        t.setStatus(Trade.TradeStatus.CLOSED);
        t.setCloseTime(LocalDateTime.now());
        walletBalance += pnl;
        tradeRepository.save(t);

        log.info("CLOSED {} {} @ {} | PnL={} | reason={} | Bal={}",
                t.getType(), t.getCurrencyPair(),
                String.format("%.5f", exit),
                String.format("%.4f", pnl), reason,
                String.format("%.2f", walletBalance));
    }

    // ── Position state ─────────────────────────────────────────────────────────

    public boolean hasOpenBuy(String pair) {
        return tradeRepository.findByStatus(Trade.TradeStatus.OPEN).stream()
                .anyMatch(t -> t.getCurrencyPair().equals(pair)
                        && t.getType() == Trade.TradeType.BUY);
    }

    public boolean hasOpenSell(String pair) {
        return tradeRepository.findByStatus(Trade.TradeStatus.OPEN).stream()
                .anyMatch(t -> t.getCurrencyPair().equals(pair)
                        && t.getType() == Trade.TradeType.SELL);
    }

    // ── Accessors ──────────────────────────────────────────────────────────────

    public void setWalletBalance(double b) { this.walletBalance = b; this.initialized = true; }
    public double getWalletBalance()       { if (!initialized) { walletBalance = initialBalance; initialized = true; } return walletBalance; }
    public double getInitialBalance()      { return initialBalance; }
    public List<Trade> getAllTrades()       { return tradeRepository.findAllByOrderByOpenTimeDesc(); }
    public List<Trade> getOpenTrades()     { return tradeRepository.findByStatus(Trade.TradeStatus.OPEN); }
    public Double getTotalPnL()            { Double t = tradeRepository.sumTotalProfitLoss(); return t != null ? t : 0.0; }
    public int getTotalTradeCount()        { return (int) tradeRepository.count(); }
    public int getWinningTradeCount()      { return (int) tradeRepository.findByStatus(Trade.TradeStatus.CLOSED).stream().filter(t -> t.getProfitLoss() > 0).count(); }
    public int getLosingTradeCount()       { return (int) tradeRepository.findByStatus(Trade.TradeStatus.CLOSED).stream().filter(t -> t.getProfitLoss() <= 0).count(); }
}
