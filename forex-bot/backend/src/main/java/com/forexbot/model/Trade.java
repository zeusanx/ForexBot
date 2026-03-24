package com.forexbot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
public class Trade {

    public enum TradeType   { BUY, SELL }
    public enum TradeStatus { OPEN, CLOSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String currencyPair;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeStatus status;

    @Column(nullable = false)
    private double entryPrice;

    private double exitPrice;

    @Column(nullable = false)
    private int lotSize;

    private double profitLoss;

    @Column(nullable = false)
    private LocalDateTime openTime;

    private LocalDateTime closeTime;

    private String signal;

    public Trade() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String currencyPair;
        private TradeType type;
        private TradeStatus status;
        private double entryPrice, exitPrice, profitLoss;
        private int lotSize;
        private LocalDateTime openTime, closeTime;
        private String signal;

        public Builder id(Long id)                  { this.id = id; return this; }
        public Builder currencyPair(String v)       { this.currencyPair = v; return this; }
        public Builder type(TradeType v)            { this.type = v; return this; }
        public Builder status(TradeStatus v)        { this.status = v; return this; }
        public Builder entryPrice(double v)         { this.entryPrice = v; return this; }
        public Builder exitPrice(double v)          { this.exitPrice = v; return this; }
        public Builder profitLoss(double v)         { this.profitLoss = v; return this; }
        public Builder lotSize(int v)               { this.lotSize = v; return this; }
        public Builder openTime(LocalDateTime v)    { this.openTime = v; return this; }
        public Builder closeTime(LocalDateTime v)   { this.closeTime = v; return this; }
        public Builder signal(String v)             { this.signal = v; return this; }

        public Trade build() {
            Trade t = new Trade();
            t.id = this.id; t.currencyPair = this.currencyPair;
            t.type = this.type; t.status = this.status;
            t.entryPrice = this.entryPrice; t.exitPrice = this.exitPrice;
            t.profitLoss = this.profitLoss; t.lotSize = this.lotSize;
            t.openTime = this.openTime; t.closeTime = this.closeTime;
            t.signal = this.signal;
            return t;
        }
    }

    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }
    public String getCurrencyPair()            { return currencyPair; }
    public void setCurrencyPair(String v)      { this.currencyPair = v; }
    public TradeType getType()                 { return type; }
    public void setType(TradeType v)           { this.type = v; }
    public TradeStatus getStatus()             { return status; }
    public void setStatus(TradeStatus v)       { this.status = v; }
    public double getEntryPrice()              { return entryPrice; }
    public void setEntryPrice(double v)        { this.entryPrice = v; }
    public double getExitPrice()               { return exitPrice; }
    public void setExitPrice(double v)         { this.exitPrice = v; }
    public int getLotSize()                    { return lotSize; }
    public void setLotSize(int v)              { this.lotSize = v; }
    public double getProfitLoss()              { return profitLoss; }
    public void setProfitLoss(double v)        { this.profitLoss = v; }
    public LocalDateTime getOpenTime()         { return openTime; }
    public void setOpenTime(LocalDateTime v)   { this.openTime = v; }
    public LocalDateTime getCloseTime()        { return closeTime; }
    public void setCloseTime(LocalDateTime v)  { this.closeTime = v; }
    public String getSignal()                  { return signal; }
    public void setSignal(String v)            { this.signal = v; }
}
