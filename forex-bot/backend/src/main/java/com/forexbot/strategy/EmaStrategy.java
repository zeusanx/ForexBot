package com.forexbot.strategy;

import com.forexbot.model.Candlestick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * EMA Crossover Strategy — Professional implementation.
 *
 * RULES (as used by real forex traders):
 * ─────────────────────────────────────
 * ENTRY:
 *   BUY  → EMA(9) crosses ABOVE EMA(21) from below  (Golden Cross)
 *          Only valid if no position is currently open.
 *
 *   SELL → EMA(9) crosses BELOW EMA(21) from above  (Death Cross)
 *          Only valid if no position is currently open.
 *
 * EXIT:
 *   Close BUY  → EMA(9) crosses BELOW EMA(21)  (same trigger as SELL entry)
 *   Close SELL → EMA(9) crosses ABOVE EMA(21)  (same trigger as BUY entry)
 *
 *   OR when SL/TP is hit (handled by MockBrokerService).
 *
 * HOLD → No crossover, or crossover but position already open in same direction.
 *
 * Signal priority:
 *   If holding BUY  and death cross → CLOSE_BUY  (exit, no new trade this cycle)
 *   If holding SELL and golden cross → CLOSE_SELL (exit, no new trade this cycle)
 *   Next cycle after close: if crossover still valid → open new position.
 */
@Component
public class EmaStrategy {

    private static final Logger log = LoggerFactory.getLogger(EmaStrategy.class);

    public enum Signal { BUY, SELL, CLOSE_BUY, CLOSE_SELL, HOLD }

    @Value("${strategy.ema.short-period:9}")
    private int shortPeriod;

    @Value("${strategy.ema.long-period:21}")
    private int longPeriod;

    // ── EMA Calculation ────────────────────────────────────────────────────────

    /**
     * Standard EMA — seeded with SMA of first 'period' prices.
     * Returns list aligned to input from index (period-1) onwards.
     */
    public List<Double> calculateEma(List<Double> prices, int period) {
        List<Double> ema = new ArrayList<>();
        if (prices == null || prices.size() < period) return ema;

        double mult = 2.0 / (period + 1);

        // Seed value = simple average of first 'period' closes
        double sum = 0;
        for (int i = 0; i < period; i++) sum += prices.get(i);
        double prev = sum / period;
        ema.add(prev);

        for (int i = period; i < prices.size(); i++) {
            prev = (prices.get(i) - prev) * mult + prev;
            ema.add(prev);
        }
        return ema;
    }

    // ── Signal Evaluation ──────────────────────────────────────────────────────

    /**
     * Evaluate the crossover signal.
     *
     * @param candles    candles in chronological order (oldest first)
     * @param hasOpenBuy  whether bot currently holds a BUY position
     * @param hasOpenSell whether bot currently holds a SELL position
     */
    public Signal evaluateSignal(List<Candlestick> candles,
                                  boolean hasOpenBuy, boolean hasOpenSell) {

        if (candles == null || candles.size() < longPeriod + 2) {
            return Signal.HOLD;
        }

        List<Double> closes = new ArrayList<>();
        for (Candlestick c : candles) closes.add(c.getClose());

        List<Double> shortEma = calculateEma(closes, shortPeriod);
        List<Double> longEma  = calculateEma(closes, longPeriod);

        if (shortEma.size() < 2 || longEma.size() < 2) return Signal.HOLD;

        // Most recent two values of each EMA
        int si = shortEma.size() - 1;
        int li = longEma.size()  - 1;

        double sCurr = shortEma.get(si);
        double sPrev = shortEma.get(si - 1);
        double lCurr = longEma.get(li);
        double lPrev = longEma.get(li - 1);

        // Crossover detection: previous bar had one order, current bar reversed
        boolean goldenCross = sPrev <= lPrev && sCurr > lCurr;  // 9 crosses above 21
        boolean deathCross  = sPrev >= lPrev && sCurr < lCurr;  // 9 crosses below 21

        log.debug("EMA9: {}→{}  EMA21: {}→{}  Golden={} Death={} hasBUY={} hasSELL={}",
                String.format("%.5f", sPrev), String.format("%.5f", sCurr),
                String.format("%.5f", lPrev), String.format("%.5f", lCurr),
                goldenCross, deathCross, hasOpenBuy, hasOpenSell);

        // ── Exit signals (priority over entries) ──────────────────────────────
        if (deathCross && hasOpenBuy)  return Signal.CLOSE_BUY;   // exit BUY on death cross
        if (goldenCross && hasOpenSell) return Signal.CLOSE_SELL; // exit SELL on golden cross

        // ── Entry signals (only when flat) ────────────────────────────────────
        if (goldenCross && !hasOpenBuy && !hasOpenSell) return Signal.BUY;
        if (deathCross  && !hasOpenBuy && !hasOpenSell) return Signal.SELL;

        return Signal.HOLD;
    }

    /** Convenience overload when no position info available */
    public Signal evaluateSignal(List<Candlestick> candles) {
        return evaluateSignal(candles, false, false);
    }

    /** Returns [emaShort, emaLong] current values */
    public double[] getCurrentEmaValues(List<Candlestick> candles) {
        if (candles == null || candles.size() < longPeriod) return new double[]{0.0, 0.0};
        List<Double> closes = new ArrayList<>();
        for (Candlestick c : candles) closes.add(c.getClose());
        List<Double> s = calculateEma(closes, shortPeriod);
        List<Double> l = calculateEma(closes, longPeriod);
        return new double[]{
            s.isEmpty() ? 0.0 : s.get(s.size() - 1),
            l.isEmpty() ? 0.0 : l.get(l.size() - 1)
        };
    }

    public int getShortPeriod() { return shortPeriod; }
    public int getLongPeriod()  { return longPeriod;  }
}
