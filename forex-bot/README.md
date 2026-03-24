# 📈 ForexBot — Automated Forex Trading Bot

A full-stack automated Forex trading system with a **Spring Boot** backend and **Android** mobile frontend, implementing the **EMA Crossover Strategy** for algorithmic signal generation and simulated trade execution.

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                   Android App (Frontend)                │
│  Dashboard │ Charts │ Trade History │ Performance       │
│                  Retrofit HTTP Client                   │
└──────────────────────┬──────────────────────────────────┘
                       │ REST API (JSON)
┌──────────────────────▼──────────────────────────────────┐
│               Spring Boot Backend (Port 8080)           │
│                                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  Market Data │  │ EMA Strategy │  │  Bot Engine  │  │
│  │   Service    │  │   (9 / 21)   │  │  Scheduler   │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
│         │                 │                  │          │
│  ┌──────▼─────────────────▼──────────────────▼───────┐  │
│  │              Mock Broker Service                  │  │
│  │  Position Mgmt │ PnL Calc │ Wallet │ Trade Log   │  │
│  └──────────────────────┬──────────────────────────┘  │
│                         │                              │
│  ┌──────────────────────▼──────────────────────────┐  │
│  │         H2 In-Memory Database (JPA)             │  │
│  │   candlesticks │ trades │ bot_state             │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

## 📁 Project Structure

```
forex-bot/
├── backend/                          # Spring Boot application
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/forexbot/
│       │   ├── ForexBotApplication.java
│       │   ├── config/
│       │   │   └── AppConfig.java           # CORS, WebClient beans
│       │   ├── controller/
│       │   │   └── BotController.java       # REST API endpoints
│       │   ├── model/
│       │   │   ├── Candlestick.java         # OHLCV entity
│       │   │   ├── Trade.java               # Trade entity
│       │   │   ├── BotState.java            # Bot state entity
│       │   │   ├── CandlestickRepository.java
│       │   │   ├── TradeRepository.java
│       │   │   └── BotStateRepository.java
│       │   ├── dto/
│       │   │   └── Dtos.java                # API response DTOs
│       │   ├── strategy/
│       │   │   └── EmaStrategy.java         # EMA calculation + signal
│       │   ├── broker/
│       │   │   └── MockBrokerService.java   # Simulated trade execution
│       │   ├── service/
│       │   │   ├── MarketDataService.java   # Alpha Vantage / mock data
│       │   │   └── BotEngineService.java    # Core trading loop
│       │   └── scheduler/
│       │       └── TradingScheduler.java    # @Scheduled trigger
│       └── resources/
│           └── application.properties
│
└── android/                          # Android Studio project
    ├── build.gradle
    ├── settings.gradle
    └── app/src/main/
        ├── AndroidManifest.xml
        ├── java/com/forexbot/
        │   ├── model/
        │   │   └── ApiModels.java           # Response POJOs (Gson)
        │   ├── api/
        │   │   ├── ForexBotApi.java         # Retrofit interface
        │   │   └── ApiClient.java           # Retrofit singleton
        │   ├── adapter/
        │   │   └── TradeAdapter.java        # RecyclerView adapter
        │   └── ui/
        │       ├── MainActivity.java        # Nav host
        │       ├── DashboardFragment.java   # Bot control + live status
        │       ├── ChartFragment.java       # Candlestick + EMA charts
        │       ├── TradesFragment.java      # Trade history list
        │       └── PerformanceFragment.java # Stats + pie chart
        └── res/
            ├── layout/
            │   ├── activity_main.xml
            │   ├── fragment_dashboard.xml
            │   ├── fragment_chart.xml
            │   ├── fragment_trades.xml
            │   ├── fragment_performance.xml
            │   └── item_trade.xml
            ├── menu/bottom_nav_menu.xml
            ├── navigation/nav_graph.xml
            ├── drawable/
            │   ├── circle_indicator.xml
            │   └── badge_background.xml
            └── values/
                ├── colors.xml
                ├── strings.xml
                └── themes.xml
```

---

## 🚀 Backend Setup (Spring Boot)

### Prerequisites
- Java 17+
- Maven 3.8+

### Steps

```bash
# 1. Navigate to backend directory
cd forex-bot/backend

# 2. (Optional) Add your Alpha Vantage API key in application.properties
#    Get a free key at: https://www.alphavantage.co/support/#api-key
#    forex.api.key=YOUR_KEY_HERE
#    Without a key, the bot runs on realistic simulated market data.

# 3. Build the project
mvn clean install

# 4. Run the Spring Boot server
mvn spring-boot:run
```

The server starts on **http://localhost:8080/api**

You can verify it's running:
```
GET http://localhost:8080/api/bot/status
```

### H2 Console (Database Inspector)
Access the in-memory database at:
```
http://localhost:8080/api/h2-console
JDBC URL: jdbc:h2:mem:forexdb
Username: sa
Password: (empty)
```

---

## 📱 Android Setup (Android Studio)

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 26+
- Running backend server

### Steps

1. **Open project** — Open the `forex-bot/android` folder in Android Studio
2. **Sync Gradle** — Click "Sync Now" when prompted
3. **Configure server URL** in `ApiClient.java`:

```java
// For Android Emulator (default):
private static final String BASE_URL = "http://10.0.2.2:8080/api/";

// For Physical Device — use your computer's local IP:
private static final String BASE_URL = "http://192.168.1.XXX:8080/api/";
```

4. **Run the app** — Select an emulator or connected device and click ▶

---

## 🔌 REST API Reference

All endpoints are prefixed with `/api`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/bot/start?pair=EUR/USD` | Start the trading bot |
| `POST` | `/bot/stop` | Stop the bot and close positions |
| `GET` | `/bot/status` | Get current bot status + EMA values |
| `GET` | `/bot/trades` | All trade history |
| `GET` | `/bot/trades/open` | Currently open positions |
| `GET` | `/bot/candles?pair=EUR/USD&limit=50` | Recent candlestick data |
| `GET` | `/bot/performance` | Win rate, PnL, return % |
| `GET` | `/bot/ema?pair=EUR/USD&limit=50` | EMA(9) and EMA(21) series |

### Example Response — `/bot/status`
```json
{
  "success": true,
  "message": "Status retrieved",
  "data": {
    "running": true,
    "walletBalance": 10045.32,
    "emaShort": 1.08512,
    "emaLong": 1.08490,
    "lastSignal": "BUY",
    "activePair": "EUR/USD",
    "totalTrades": 12,
    "totalPnL": 45.32,
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

---

## ⚙️ Configuration (`application.properties`)

| Property | Default | Description |
|----------|---------|-------------|
| `forex.api.key` | `demo` | Alpha Vantage API key (optional) |
| `forex.default.pair` | `EUR/USD` | Default currency pair |
| `strategy.ema.short-period` | `9` | Short EMA period |
| `strategy.ema.long-period` | `21` | Long EMA period |
| `bot.scheduler.interval` | `60000` | Cycle interval in ms (60s) |
| `broker.initial.balance` | `10000.00` | Starting wallet balance ($) |
| `broker.trade.lot-size` | `1000` | Units per trade |
| `broker.spread` | `0.0002` | Simulated bid/ask spread |

---

## 📊 EMA Strategy Logic

```
Short EMA (9-period) crosses ABOVE Long EMA (21-period) → 🟢 BUY Signal
Short EMA (9-period) crosses BELOW Long EMA (21-period) → 🔴 SELL Signal
No crossover detected                                   → 🟡 HOLD
```

**EMA Formula:**
```
EMA = Price × (2 / Period + 1) + Previous_EMA × (1 − 2 / Period + 1)
```

The bot seeds 31+ historical candles on startup for EMA warm-up before generating signals.

---

## 📱 Android Screens

| Screen | Description |
|--------|-------------|
| **Dashboard** | Live bot status, balance, PnL, EMA values, START/STOP controls |
| **Chart** | Candlestick OHLC chart + EMA(9)/EMA(21) line chart overlay |
| **Trades** | Full trade history with entry/exit prices, PnL per trade (pull-to-refresh) |
| **Performance** | Win rate pie chart, return %, balance summary, trade stats |

---

## 🔮 Extending the Project

| Feature | Where to Add |
|---------|-------------|
| New strategy (RSI, MACD) | Create new class in `strategy/` implementing same signal interface |
| Real broker (OANDA, IG) | Replace `MockBrokerService` with real API calls |
| WebSocket live feed | Add `spring-boot-starter-websocket` + subscribe in Android |
| Push notifications | Add Firebase FCM for signal alerts to mobile |
| Database persistence | Switch `application.properties` from H2 to PostgreSQL/MySQL |
| Multi-pair support | Bot engine already parameterized by pair — extend scheduler |

---

## 📦 Dependencies

### Backend
| Dependency | Purpose |
|-----------|---------|
| Spring Boot 3.2 | Core framework |
| Spring Data JPA | ORM / database |
| H2 Database | In-memory SQL |
| Spring WebFlux | Reactive HTTP client (Alpha Vantage) |
| Lombok | Boilerplate reduction |

### Android
| Dependency | Purpose |
|-----------|---------|
| Retrofit 2.9 | HTTP API client |
| Gson | JSON parsing |
| MPAndroidChart 3.1 | Candlestick + line + pie charts |
| Navigation Component | Fragment navigation |
| Material Components | UI components |
| OkHttp Logging | API request logging |

---

## ⚠️ Disclaimer

This project is for **educational purposes only**. The mock broker does not connect to real financial markets. Do not use this system for actual trading without proper licensing, risk management, and legal compliance.
