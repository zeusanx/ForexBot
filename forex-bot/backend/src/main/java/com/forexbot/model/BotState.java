package com.forexbot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bot_state")
public class BotState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String stateKey;

    @Column(nullable = false)
    private boolean running;

    @Column(nullable = false)
    private double walletBalance;

    private double currentEmaShort;
    private double currentEmaLong;
    private String lastSignal;
    private String activePair;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public BotState() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String stateKey, lastSignal, activePair;
        private boolean running;
        private double walletBalance, currentEmaShort, currentEmaLong;
        private LocalDateTime updatedAt;

        public Builder id(Long id)                   { this.id = id; return this; }
        public Builder stateKey(String v)            { this.stateKey = v; return this; }
        public Builder running(boolean v)            { this.running = v; return this; }
        public Builder walletBalance(double v)       { this.walletBalance = v; return this; }
        public Builder currentEmaShort(double v)     { this.currentEmaShort = v; return this; }
        public Builder currentEmaLong(double v)      { this.currentEmaLong = v; return this; }
        public Builder lastSignal(String v)          { this.lastSignal = v; return this; }
        public Builder activePair(String v)          { this.activePair = v; return this; }
        public Builder updatedAt(LocalDateTime v)    { this.updatedAt = v; return this; }

        public BotState build() {
            BotState s = new BotState();
            s.id = this.id; s.stateKey = this.stateKey;
            s.running = this.running; s.walletBalance = this.walletBalance;
            s.currentEmaShort = this.currentEmaShort; s.currentEmaLong = this.currentEmaLong;
            s.lastSignal = this.lastSignal; s.activePair = this.activePair;
            s.updatedAt = this.updatedAt;
            return s;
        }
    }

    public Long getId()                         { return id; }
    public void setId(Long id)                  { this.id = id; }
    public String getStateKey()                 { return stateKey; }
    public void setStateKey(String v)           { this.stateKey = v; }
    public boolean isRunning()                  { return running; }
    public void setRunning(boolean v)           { this.running = v; }
    public double getWalletBalance()            { return walletBalance; }
    public void setWalletBalance(double v)      { this.walletBalance = v; }
    public double getCurrentEmaShort()          { return currentEmaShort; }
    public void setCurrentEmaShort(double v)    { this.currentEmaShort = v; }
    public double getCurrentEmaLong()           { return currentEmaLong; }
    public void setCurrentEmaLong(double v)     { this.currentEmaLong = v; }
    public String getLastSignal()               { return lastSignal; }
    public void setLastSignal(String v)         { this.lastSignal = v; }
    public String getActivePair()               { return activePair; }
    public void setActivePair(String v)         { this.activePair = v; }
    public LocalDateTime getUpdatedAt()         { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v)   { this.updatedAt = v; }
}
