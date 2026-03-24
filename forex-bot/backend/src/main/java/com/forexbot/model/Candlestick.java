package com.forexbot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "candlesticks")
public class Candlestick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String currencyPair;

    @Column(nullable = false)
    private double open;

    @Column(nullable = false)
    private double high;

    @Column(nullable = false)
    private double low;

    @Column(nullable = false)
    private double close;

    @Column(nullable = false)
    private long volume;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public Candlestick() {}

    public Candlestick(String currencyPair, double open, double high, double low,
                       double close, long volume, LocalDateTime timestamp) {
        this.currencyPair = currencyPair;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.timestamp = timestamp;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String currencyPair;
        private double open, high, low, close;
        private long volume;
        private LocalDateTime timestamp;

        public Builder id(Long id)                { this.id = id; return this; }
        public Builder currencyPair(String v)     { this.currencyPair = v; return this; }
        public Builder open(double v)             { this.open = v; return this; }
        public Builder high(double v)             { this.high = v; return this; }
        public Builder low(double v)              { this.low = v; return this; }
        public Builder close(double v)            { this.close = v; return this; }
        public Builder volume(long v)             { this.volume = v; return this; }
        public Builder timestamp(LocalDateTime v) { this.timestamp = v; return this; }

        public Candlestick build() {
            Candlestick c = new Candlestick();
            c.id = this.id; c.currencyPair = this.currencyPair;
            c.open = this.open; c.high = this.high;
            c.low = this.low; c.close = this.close;
            c.volume = this.volume; c.timestamp = this.timestamp;
            return c;
        }
    }

    public Long getId()                       { return id; }
    public void setId(Long id)                { this.id = id; }
    public String getCurrencyPair()           { return currencyPair; }
    public void setCurrencyPair(String v)     { this.currencyPair = v; }
    public double getOpen()                   { return open; }
    public void setOpen(double v)             { this.open = v; }
    public double getHigh()                   { return high; }
    public void setHigh(double v)             { this.high = v; }
    public double getLow()                    { return low; }
    public void setLow(double v)              { this.low = v; }
    public double getClose()                  { return close; }
    public void setClose(double v)            { this.close = v; }
    public long getVolume()                   { return volume; }
    public void setVolume(long v)             { this.volume = v; }
    public LocalDateTime getTimestamp()       { return timestamp; }
    public void setTimestamp(LocalDateTime v) { this.timestamp = v; }
}
