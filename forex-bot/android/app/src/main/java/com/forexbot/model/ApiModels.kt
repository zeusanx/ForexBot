package com.forexbot.model
import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = "",
    @SerializedName("data") val data: T? = null
)

data class BotStatus(
    @SerializedName("running") val running: Boolean = false,
    @SerializedName("walletBalance") val walletBalance: Double = 0.0,
    @SerializedName("emaShort") val emaShort: Double = 0.0,
    @SerializedName("emaLong") val emaLong: Double = 0.0,
    @SerializedName("lastSignal") val lastSignal: String = "NONE",
    @SerializedName("activePair") val activePair: String = "EUR/USD",
    @SerializedName("totalTrades") val totalTrades: Int = 0,
    @SerializedName("totalPnL") val totalPnL: Double = 0.0,
    @SerializedName("updatedAt") val updatedAt: String = "",
    @SerializedName("targetProfit") val targetProfit: Double = 20.0,
    @SerializedName("maxLoss") val maxLoss: Double = 7.0
)

data class Trade(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("currencyPair") val currencyPair: String = "",
    @SerializedName("type") val type: String = "",
    @SerializedName("status") val status: String = "",
    @SerializedName("entryPrice") val entryPrice: Double = 0.0,
    @SerializedName("exitPrice") val exitPrice: Double = 0.0,
    @SerializedName("lotSize") val lotSize: Int = 0,
    @SerializedName("profitLoss") val profitLoss: Double = 0.0,
    @SerializedName("openTime") val openTime: String = "",
    @SerializedName("closeTime") val closeTime: String = "",
    @SerializedName("signal") val signal: String = ""
)

data class Candlestick(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("currencyPair") val currencyPair: String = "",
    @SerializedName("open") val open: Double = 0.0,
    @SerializedName("high") val high: Double = 0.0,
    @SerializedName("low") val low: Double = 0.0,
    @SerializedName("close") val close: Double = 0.0,
    @SerializedName("volume") val volume: Long = 0,
    @SerializedName("timestamp") val timestamp: String = ""
)

data class Performance(
    @SerializedName("totalPnL") val totalPnL: Double = 0.0,
    @SerializedName("totalTrades") val totalTrades: Int = 0,
    @SerializedName("winningTrades") val winningTrades: Int = 0,
    @SerializedName("losingTrades") val losingTrades: Int = 0,
    @SerializedName("winRate") val winRate: Double = 0.0,
    @SerializedName("currentBalance") val currentBalance: Double = 0.0,
    @SerializedName("initialBalance") val initialBalance: Double = 0.0,
    @SerializedName("returnPercent") val returnPercent: Double = 0.0
)

data class EmaData(
    @SerializedName("emaShortValues") val emaShortValues: List<Double> = emptyList(),
    @SerializedName("emaLongValues") val emaLongValues: List<Double> = emptyList(),
    @SerializedName("timestamps") val timestamps: List<String> = emptyList(),
    @SerializedName("shortPeriod") val shortPeriod: Int = 9,
    @SerializedName("longPeriod") val longPeriod: Int = 21
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class RegisterRequest(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class AuthResponse(
    @SerializedName("token") val token: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("email") val email: String = ""
)
