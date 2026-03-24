package com.forexbot.api

import com.forexbot.model.*
import retrofit2.http.*

interface ForexBotApi {
    @POST("bot/start")
    suspend fun startBot(
        @Query("pair") pair: String = "EUR/USD",
        @Query("targetProfit") targetProfit: Double = 20.0,
        @Query("maxLoss") maxLoss: Double = 7.0
    ): ApiResponse<BotStatus>
    @POST("bot/stop")
    suspend fun stopBot(): ApiResponse<BotStatus>
    @GET("bot/status")
    suspend fun getStatus(): ApiResponse<BotStatus>
    @GET("bot/trades")
    suspend fun getAllTrades(): ApiResponse<List<Trade>>
    @GET("bot/candles")
    suspend fun getCandles(@Query("pair") pair: String = "EUR/USD", @Query("limit") limit: Int = 80): ApiResponse<List<Candlestick>>
    @GET("bot/performance")
    suspend fun getPerformance(): ApiResponse<Performance>
    @GET("bot/ema")
    suspend fun getEmaData(@Query("pair") pair: String = "EUR/USD", @Query("limit") limit: Int = 80): ApiResponse<EmaData>
    @GET("bot/price")
    suspend fun getLivePrice(
        @Query("pair") pair: String = "EUR/USD"
    ): ApiResponse<Map<String, Any>>
}
