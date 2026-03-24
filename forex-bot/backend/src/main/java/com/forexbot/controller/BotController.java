package com.forexbot.controller;

import com.forexbot.broker.MockBrokerService;
import com.forexbot.dto.Dtos.*;
import com.forexbot.model.BotState;
import com.forexbot.model.Candlestick;
import com.forexbot.model.Trade;
import com.forexbot.service.BotEngineService;
import com.forexbot.service.MarketDataService;
import com.forexbot.strategy.EmaStrategy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller exposing all bot control and monitoring APIs.
 */
@RestController
@RequestMapping("/bot")
@CrossOrigin(origins = "*")
public class BotController {

    private final BotEngineService botEngineService;
    private final MockBrokerService brokerService;
    private final MarketDataService marketDataService;
    private final EmaStrategy emaStrategy;

    public BotController(BotEngineService botEngineService,
                         MockBrokerService brokerService,
                         MarketDataService marketDataService,
                         EmaStrategy emaStrategy) {
        this.botEngineService  = botEngineService;
        this.brokerService     = brokerService;
        this.marketDataService = marketDataService;
        this.emaStrategy       = emaStrategy;
    }

    // ─── Bot Control ──────────────────────────────────────

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<BotStatusResponse>> startBot(
            @RequestParam(required = false) String pair,
            @RequestParam(defaultValue = "20.0") double targetProfit,
            @RequestParam(defaultValue = "7.0")  double maxLoss) {
        BotState state = botEngineService.startBot(pair, targetProfit, maxLoss);
        return ResponseEntity.ok(ApiResponse.<BotStatusResponse>builder()
                .success(true)
                .message("Bot started successfully")
                .data(toStatusResponse(state))
                .build());
    }

    @PostMapping("/stop")
    public ResponseEntity<ApiResponse<BotStatusResponse>> stopBot() {
        BotState state = botEngineService.stopBot();
        return ResponseEntity.ok(ApiResponse.<BotStatusResponse>builder()
                .success(true)
                .message("Bot stopped successfully")
                .data(toStatusResponse(state))
                .build());
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<BotStatusResponse>> getStatus() {
        BotState state = botEngineService.getBotState();
        return ResponseEntity.ok(ApiResponse.<BotStatusResponse>builder()
                .success(true)
                .message("Status retrieved")
                .data(toStatusResponse(state))
                .build());
    }

    // ─── Trade History ────────────────────────────────────

    @GetMapping("/trades")
    public ResponseEntity<ApiResponse<List<TradeResponse>>> getAllTrades() {
        List<TradeResponse> trades = brokerService.getAllTrades().stream()
                .map(this::toTradeResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.<List<TradeResponse>>builder()
                .success(true)
                .message("Trades retrieved")
                .data(trades)
                .build());
    }

    @GetMapping("/trades/open")
    public ResponseEntity<ApiResponse<List<TradeResponse>>> getOpenTrades() {
        List<TradeResponse> trades = brokerService.getOpenTrades().stream()
                .map(this::toTradeResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.<List<TradeResponse>>builder()
                .success(true)
                .message("Open trades retrieved")
                .data(trades)
                .build());
    }

    // ─── Market Data ──────────────────────────────────────

    @GetMapping("/candles")
    public ResponseEntity<ApiResponse<List<CandlestickResponse>>> getCandles(
            @RequestParam(defaultValue = "EUR/USD") String pair,
            @RequestParam(defaultValue = "50") int limit) {
        List<CandlestickResponse> candles = marketDataService.getLatestCandles(pair, limit).stream()
                .map(this::toCandleResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.<List<CandlestickResponse>>builder()
                .success(true)
                .message("Candles retrieved")
                .data(candles)
                .build());
    }

    // ─── Performance ──────────────────────────────────────

    @GetMapping("/performance")
    public ResponseEntity<ApiResponse<PerformanceResponse>> getPerformance() {
        double totalPnL  = brokerService.getTotalPnL();
        int    total     = brokerService.getTotalTradeCount();
        int    winning   = brokerService.getWinningTradeCount();
        int    losing    = brokerService.getLosingTradeCount();
        double balance   = brokerService.getWalletBalance();
        double initialBal = brokerService.getInitialBalance();
        double returnPct  = initialBal > 0 ? ((balance - initialBal) / initialBal) * 100 : 0;
        double winRate    = total > 0 ? ((double) winning / total) * 100 : 0;

        PerformanceResponse perf = PerformanceResponse.builder()
                .totalPnL(totalPnL)
                .totalTrades(total)
                .winningTrades(winning)
                .losingTrades(losing)
                .winRate(Math.round(winRate * 100.0) / 100.0)
                .currentBalance(balance)
                .initialBalance(initialBal)
                .returnPercent(Math.round(returnPct * 100.0) / 100.0)
                .build();

        return ResponseEntity.ok(ApiResponse.<PerformanceResponse>builder()
                .success(true)
                .message("Performance retrieved")
                .data(perf)
                .build());
    }

    // ─── EMA Data ─────────────────────────────────────────

    @GetMapping("/ema")
    public ResponseEntity<ApiResponse<EmaDataResponse>> getEmaData(
            @RequestParam(defaultValue = "EUR/USD") String pair,
            @RequestParam(defaultValue = "50") int limit) {

        List<Candlestick> candles = marketDataService.getLatestCandles(pair, limit);
        List<Double> closes = candles.stream()
                .map(Candlestick::getClose)
                .collect(Collectors.toList());
        List<String> timestamps = candles.stream()
                .map(c -> c.getTimestamp().toString())
                .collect(Collectors.toList());

        List<Double> shortEma = emaStrategy.calculateEma(closes, emaStrategy.getShortPeriod());
        List<Double> longEma  = emaStrategy.calculateEma(closes, emaStrategy.getLongPeriod());

        EmaDataResponse emaData = EmaDataResponse.builder()
                .emaShortValues(shortEma)
                .emaLongValues(longEma)
                .timestamps(timestamps)
                .shortPeriod(emaStrategy.getShortPeriod())
                .longPeriod(emaStrategy.getLongPeriod())
                .build();

        return ResponseEntity.ok(ApiResponse.<EmaDataResponse>builder()
                .success(true)
                .message("EMA data retrieved")
                .data(emaData)
                .build());
    }

    // ─── Mappers ──────────────────────────────────────────

    private BotStatusResponse toStatusResponse(BotState state) {
        return BotStatusResponse.builder()
                .running(state.isRunning())
                .walletBalance(state.getWalletBalance())
                .emaShort(state.getCurrentEmaShort())
                .emaLong(state.getCurrentEmaLong())
                .lastSignal(state.getLastSignal())
                .activePair(state.getActivePair())
                .totalTrades(brokerService.getTotalTradeCount())
                .totalPnL(brokerService.getTotalPnL())
                .updatedAt(state.getUpdatedAt())
                .targetProfit(brokerService.getTargetProfit())
                .maxLoss(brokerService.getMaxLoss())
                .build();
    }

    private TradeResponse toTradeResponse(Trade t) {
        return TradeResponse.builder()
                .id(t.getId())
                .currencyPair(t.getCurrencyPair())
                .type(t.getType().name())
                .status(t.getStatus().name())
                .entryPrice(t.getEntryPrice())
                .exitPrice(t.getExitPrice())
                .lotSize(t.getLotSize())
                .profitLoss(t.getProfitLoss())
                .openTime(t.getOpenTime())
                .closeTime(t.getCloseTime())
                .signal(t.getSignal())
                .build();
    }

    private CandlestickResponse toCandleResponse(Candlestick c) {
        return CandlestickResponse.builder()
                .id(c.getId())
                .currencyPair(c.getCurrencyPair())
                .open(c.getOpen())
                .high(c.getHigh())
                .low(c.getLow())
                .close(c.getClose())
                .volume(c.getVolume())
                .timestamp(c.getTimestamp())
                .build();
    }
    // ─── Live Price ──────────────────────────────────────────────────────────

    @GetMapping("/price")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getLivePrice(
            @RequestParam(defaultValue = "EUR/USD") String pair) {
        java.util.List<com.forexbot.model.Candlestick> candles = marketDataService.getLatestCandles(pair, 1);
        double close = candles.isEmpty() ? 0.0 : candles.get(0).getClose();
        double spread = 0.0002;
        java.util.Map<String, Object> priceData = new java.util.HashMap<>();
        priceData.put("pair", pair);
        priceData.put("mid", close);
        priceData.put("bid", Math.round((close - spread / 2) * 100000.0) / 100000.0);
        priceData.put("ask", Math.round((close + spread / 2) * 100000.0) / 100000.0);
        priceData.put("spread", spread);
        return ResponseEntity.ok(ApiResponse.<java.util.Map<String, Object>>builder()
                .success(true).message("price").data(priceData).build());
    }


}

