package com.forexbot.viewmodel

import androidx.lifecycle.*
import com.forexbot.api.ApiClient
import com.forexbot.model.*
import kotlinx.coroutines.*

class BotViewModel : ViewModel() {
    private val api = ApiClient.api

    private val _status      = MutableLiveData<BotStatus>()
    val status: LiveData<BotStatus> = _status

    private val _trades      = MutableLiveData<List<Trade>>()
    val trades: LiveData<List<Trade>> = _trades

    private val _candles     = MutableLiveData<List<Candlestick>>()
    val candles: LiveData<List<Candlestick>> = _candles

    private val _emaData     = MutableLiveData<EmaData>()
    val emaData: LiveData<EmaData> = _emaData

    private val _performance = MutableLiveData<Performance>()
    val performance: LiveData<Performance> = _performance

    private val _error       = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _loading     = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    // Live tick simulation (ask/bid spread)
    private val _ask = MutableLiveData<Double>()
    val ask: LiveData<Double> = _ask

    private val _bid = MutableLiveData<Double>()
    val bid: LiveData<Double> = _bid

    private val _priceUp = MutableLiveData<Boolean>()
    val priceUp: LiveData<Boolean> = _priceUp

    private var lastClose = 0.0

    fun fetchStatusAndTick() = viewModelScope.launch {
        runCatching { api.getStatus() }.onSuccess { resp ->
            resp.data?.let { d -> _status.postValue(d) }
        }.onFailure { _error.postValue("Server unreachable") }
    }

    fun fetchLivePrice() = viewModelScope.launch {
        runCatching { api.getLivePrice("EUR/USD") }.onSuccess { resp ->
            resp.data?.let { d ->
                val mid = (d["mid"] as? Double) ?: 0.0
                if (mid > 0) updateLiveTick(mid)
            }
        }.onFailure {}
    }

        fun fetchStatus() = viewModelScope.launch {
        runCatching { api.getStatus() }
            .onSuccess { it.data?.let { d -> _status.postValue(d) } }
            .onFailure { _error.postValue("Server unreachable") }
    }

    fun startBot(targetProfit: Double = 20.0, maxLoss: Double = 7.0) = viewModelScope.launch {
        _loading.postValue(true)
        runCatching { api.startBot("EUR/USD", targetProfit, maxLoss) }
            .onSuccess { it.data?.let { d -> _status.postValue(d) } }
            .onFailure { _error.postValue("Failed to start: ${it.message}") }
        _loading.postValue(false)
    }

    fun stopBot() = viewModelScope.launch {
        _loading.postValue(true)
        runCatching { api.stopBot() }
            .onSuccess { it.data?.let { d -> _status.postValue(d) } }
            .onFailure { _error.postValue("Failed to stop: ${it.message}") }
        _loading.postValue(false)
    }

    fun fetchTrades() = viewModelScope.launch {
        runCatching { api.getAllTrades() }
            .onSuccess { it.data?.let { d -> _trades.postValue(d) } }
            .onFailure { _error.postValue("Failed to load trades") }
    }

    fun fetchChartData() = viewModelScope.launch {
        runCatching { api.getCandles("EUR/USD", 80) }.onSuccess { resp ->
            resp.data?.let { d ->
                _candles.postValue(d)
                // Use the LATEST CANDLE CLOSE as the live mid-price for bid/ask
                d.lastOrNull()?.let { latest -> updateLiveTick(latest.close) }
            }
        }.onFailure {}
        runCatching { api.getEmaData("EUR/USD", 80) }
            .onSuccess { it.data?.let { d -> _emaData.postValue(d) } }
            .onFailure {}
    }

    fun fetchPerformance() = viewModelScope.launch {
        runCatching { api.getPerformance() }
            .onSuccess { it.data?.let { d -> _performance.postValue(d) } }
            .onFailure { _error.postValue("Failed to load performance") }
    }

    fun updateLiveTick(close: Double) {
        val spread = 0.00020
        val ask = close + spread / 2
        val bid = close - spread / 2
        _priceUp.postValue(close >= lastClose)
        _ask.postValue(ask)
        _bid.postValue(bid)
        lastClose = close
    }
}
