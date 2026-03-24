package com.forexbot.dto;

import java.time.LocalDateTime;
import java.util.List;

public class Dtos {

    public static class BotStatusResponse {
        public boolean running;
        public double walletBalance;
        public double emaShort;
        public double emaLong;
        public String lastSignal;
        public String activePair;
        public int totalTrades;
        public double totalPnL;
        public LocalDateTime updatedAt;
        public double targetProfit;
        public double maxLoss;

        public static Builder builder() { return new Builder(); }
        public static class Builder {
            private final BotStatusResponse o = new BotStatusResponse();
            public Builder running(boolean v)           { o.running = v; return this; }
            public Builder walletBalance(double v)      { o.walletBalance = v; return this; }
            public Builder emaShort(double v)           { o.emaShort = v; return this; }
            public Builder emaLong(double v)            { o.emaLong = v; return this; }
            public Builder lastSignal(String v)         { o.lastSignal = v; return this; }
            public Builder activePair(String v)         { o.activePair = v; return this; }
            public Builder totalTrades(int v)           { o.totalTrades = v; return this; }
            public Builder totalPnL(double v)           { o.totalPnL = v; return this; }
            public Builder updatedAt(LocalDateTime v)   { o.updatedAt = v; return this; }
            public Builder targetProfit(double v)       { o.targetProfit = v; return this; }
            public Builder maxLoss(double v)            { o.maxLoss = v; return this; }
            public BotStatusResponse build()            { return o; }
        }
    }

    public static class TradeResponse {
        public Long id;
        public String currencyPair, type, status, signal;
        public double entryPrice, exitPrice, profitLoss;
        public int lotSize;
        public LocalDateTime openTime, closeTime;

        public static Builder builder() { return new Builder(); }
        public static class Builder {
            private final TradeResponse o = new TradeResponse();
            public Builder id(Long v)                   { o.id = v; return this; }
            public Builder currencyPair(String v)       { o.currencyPair = v; return this; }
            public Builder type(String v)               { o.type = v; return this; }
            public Builder status(String v)             { o.status = v; return this; }
            public Builder entryPrice(double v)         { o.entryPrice = v; return this; }
            public Builder exitPrice(double v)          { o.exitPrice = v; return this; }
            public Builder lotSize(int v)               { o.lotSize = v; return this; }
            public Builder profitLoss(double v)         { o.profitLoss = v; return this; }
            public Builder openTime(LocalDateTime v)    { o.openTime = v; return this; }
            public Builder closeTime(LocalDateTime v)   { o.closeTime = v; return this; }
            public Builder signal(String v)             { o.signal = v; return this; }
            public TradeResponse build()                { return o; }
        }
    }

    public static class CandlestickResponse {
        public Long id;
        public String currencyPair;
        public double open, high, low, close;
        public long volume;
        public LocalDateTime timestamp;

        public static Builder builder() { return new Builder(); }
        public static class Builder {
            private final CandlestickResponse o = new CandlestickResponse();
            public Builder id(Long v)                   { o.id = v; return this; }
            public Builder currencyPair(String v)       { o.currencyPair = v; return this; }
            public Builder open(double v)               { o.open = v; return this; }
            public Builder high(double v)               { o.high = v; return this; }
            public Builder low(double v)                { o.low = v; return this; }
            public Builder close(double v)              { o.close = v; return this; }
            public Builder volume(long v)               { o.volume = v; return this; }
            public Builder timestamp(LocalDateTime v)   { o.timestamp = v; return this; }
            public CandlestickResponse build()          { return o; }
        }
    }

    public static class PerformanceResponse {
        public double totalPnL, winRate, currentBalance, initialBalance, returnPercent;
        public int totalTrades, winningTrades, losingTrades;

        public static Builder builder() { return new Builder(); }
        public static class Builder {
            private final PerformanceResponse o = new PerformanceResponse();
            public Builder totalPnL(double v)           { o.totalPnL = v; return this; }
            public Builder winRate(double v)            { o.winRate = v; return this; }
            public Builder currentBalance(double v)     { o.currentBalance = v; return this; }
            public Builder initialBalance(double v)     { o.initialBalance = v; return this; }
            public Builder returnPercent(double v)      { o.returnPercent = v; return this; }
            public Builder totalTrades(int v)           { o.totalTrades = v; return this; }
            public Builder winningTrades(int v)         { o.winningTrades = v; return this; }
            public Builder losingTrades(int v)          { o.losingTrades = v; return this; }
            public PerformanceResponse build()          { return o; }
        }
    }

    public static class ApiResponse<T> {
        public boolean success;
        public String message;
        public T data;

        public static <T> Builder<T> builder() { return new Builder<>(); }
        public static class Builder<T> {
            private final ApiResponse<T> o = new ApiResponse<>();
            public Builder<T> success(boolean v)   { o.success = v; return this; }
            public Builder<T> message(String v)    { o.message = v; return this; }
            public Builder<T> data(T v)            { o.data = v; return this; }
            public ApiResponse<T> build()          { return o; }
        }
    }

    public static class EmaDataResponse {
        public List<Double> emaShortValues, emaLongValues;
        public List<String> timestamps;
        public int shortPeriod, longPeriod;

        public static Builder builder() { return new Builder(); }
        public static class Builder {
            private final EmaDataResponse o = new EmaDataResponse();
            public Builder emaShortValues(List<Double> v)  { o.emaShortValues = v; return this; }
            public Builder emaLongValues(List<Double> v)   { o.emaLongValues = v; return this; }
            public Builder timestamps(List<String> v)      { o.timestamps = v; return this; }
            public Builder shortPeriod(int v)              { o.shortPeriod = v; return this; }
            public Builder longPeriod(int v)               { o.longPeriod = v; return this; }
            public EmaDataResponse build()                 { return o; }
        }
    }
}
