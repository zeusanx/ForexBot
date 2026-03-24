package com.forexbot.ui

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.forexbot.databinding.FragmentChartBinding
import com.forexbot.model.Candlestick
import com.forexbot.model.EmaData
import com.forexbot.model.Trade
import com.forexbot.viewmodel.BotViewModel
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import java.util.Locale

class ChartFragment : Fragment() {

    private var _binding: FragmentChartBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BotViewModel by activityViewModels()
    private val tradeAdapter = com.forexbot.adapter.TradeAdapter()

    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            viewModel.fetchChartData()
            viewModel.fetchTrades()
            handler.postDelayed(this, 15_000)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?) =
        FragmentChartBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupChart()

        binding.rvChartTrades.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(context)
        binding.rvChartTrades.adapter = tradeAdapter

        viewModel.candles.observe(viewLifecycleOwner) { c ->
            viewModel.emaData.value?.let { e -> renderChart(c, e) }
            updateOhlcBar(c)
        }
        viewModel.emaData.observe(viewLifecycleOwner) { e ->
            viewModel.candles.value?.let { c -> renderChart(c, e) }
        }
        viewModel.trades.observe(viewLifecycleOwner) { trades ->
            tradeAdapter.setTrades(trades.take(10))
            drawTradeMarkers(trades)
        }

        binding.btnRefresh.setOnClickListener {
            viewModel.fetchChartData()
            viewModel.fetchTrades()
        }
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onResume() { super.onResume(); handler.post(refreshRunnable) }
    override fun onPause()  { super.onPause();  handler.removeCallbacks(refreshRunnable) }

    // ── Chart setup ────────────────────────────────────────────────────────────

    private fun setupChart() {
        val chart = binding.candlestickChart
        chart.setBackgroundColor(Color.parseColor("#131722"))
        chart.description.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.setPinchZoom(true)
        chart.isScaleXEnabled = true
        chart.isScaleYEnabled = false
        chart.isHighlightPerDragEnabled = false
        chart.setMaxVisibleValueCount(200)
        // Draw candles BELOW the lines so EMA is visible on top
        chart.drawOrder = arrayOf(
            CombinedChart.DrawOrder.CANDLE,
            CombinedChart.DrawOrder.LINE
        )

        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            textColor = Color.parseColor("#787B86")
            textSize = 9f
            gridColor = Color.parseColor("#1E2433")
            gridLineWidth = 0.5f
            setDrawAxisLine(true)
            axisLineColor = Color.parseColor("#2A3147")
            granularity = 1f
            setAvoidFirstLastClipping(true)
            setLabelCount(5, true)
        }

        chart.axisLeft.apply {
            textColor = Color.parseColor("#787B86")
            textSize = 9f
            gridColor = Color.parseColor("#1E2433")
            gridLineWidth = 0.5f
            setDrawAxisLine(true)
            axisLineColor = Color.parseColor("#2A3147")
            setLabelCount(8, false)
            setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float) =
                    String.format(Locale.US, "%.5f", value)
            }
        }
        chart.axisRight.isEnabled = false

        chart.legend.apply {
            isEnabled = true
            textColor = Color.parseColor("#787B86")
            textSize = 10f
            form = Legend.LegendForm.LINE
            formSize = 14f
            horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
            verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            orientation = Legend.LegendOrientation.HORIZONTAL
            setDrawInside(false)
        }
    }

    // ── Render standard OHLC candles + EMA lines ───────────────────────────────

    private fun renderChart(candles: List<Candlestick>, ema: EmaData) {
        if (candles.size < 3) return

        val labels = candles.map { c ->
            val ts = c.timestamp
            if (ts.length >= 16) ts.substring(11, 16) else ""
        }

        // ── Standard OHLC candle entries (NOT Heikin-Ashi) ───────────────────
        // Each CandleEntry uses the ACTUAL open/high/low/close from the server
        val candleEntries = candles.mapIndexed { i, c ->
            CandleEntry(
                i.toFloat(),
                c.high.toFloat(),   // shadowHigh
                c.low.toFloat(),    // shadowLow
                c.open.toFloat(),   // open
                c.close.toFloat()   // close
            )
        }

        val candleSet = CandleDataSet(candleEntries, "EUR/USD").apply {
            axisDependency = YAxis.AxisDependency.LEFT

            // Wick (shadow) — grey thin line
            shadowColor = Color.parseColor("#9E9E9E")
            shadowWidth = 0.8f

            // Bearish candle body (close < open) — red filled
            decreasingColor      = Color.parseColor("#EF5350")
            decreasingPaintStyle = Paint.Style.FILL

            // Bullish candle body (close > open) — teal filled
            increasingColor      = Color.parseColor("#26A69A")
            increasingPaintStyle = Paint.Style.FILL

            // Doji (close == open) — neutral grey
            neutralColor = Color.parseColor("#B0BEC5")

            setDrawValues(false)
            highLightColor = Color.parseColor("#FFFFFF")
            setDrawHighlightIndicators(false)
        }

        // ── EMA series — correctly offset to align with candles ──────────────
        // EMA series is shorter than candles because it needs period candles for warmup.
        // Offset = how many candles come BEFORE the first EMA value.
        val shortOffset = (candles.size - ema.emaShortValues.size).coerceAtLeast(0)
        val longOffset  = (candles.size - ema.emaLongValues.size).coerceAtLeast(0)

        val shortEntries = ema.emaShortValues.mapIndexed { i, v ->
            Entry((i + shortOffset).toFloat(), v.toFloat())
        }
        val longEntries = ema.emaLongValues.mapIndexed { i, v ->
            Entry((i + longOffset).toFloat(), v.toFloat())
        }

        // EMA(9) — bright orange, 2pt
        val shortSet = LineDataSet(shortEntries, "EMA(${ema.shortPeriod})").apply {
            axisDependency = YAxis.AxisDependency.LEFT
            color = Color.parseColor("#FF9800")
            lineWidth = 2.0f
            setDrawCircles(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.08f
            isHighlightEnabled = false
        }

        // EMA(21) — bright blue, 2pt
        val longSet = LineDataSet(longEntries, "EMA(${ema.longPeriod})").apply {
            axisDependency = YAxis.AxisDependency.LEFT
            color = Color.parseColor("#2196F3")
            lineWidth = 2.0f
            setDrawCircles(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.08f
            isHighlightEnabled = false
        }

        // ── Combine and render ───────────────────────────────────────────────
        val combined = CombinedData()
        combined.setData(CandleData(candleSet))
        combined.setData(LineData(shortSet, longSet))

        // Auto-scale Y to visible candle range + 10% padding
        val recent = candles.takeLast(minOf(candles.size, 60))
        val minP   = recent.minOf { it.low }.toFloat()
        val maxP   = recent.maxOf { it.high }.toFloat()
        val pad    = (maxP - minP) * 0.12f
        binding.candlestickChart.axisLeft.axisMinimum = minP - pad
        binding.candlestickChart.axisLeft.axisMaximum = maxP + pad

        binding.candlestickChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        binding.candlestickChart.data = combined
        binding.candlestickChart.moveViewToX(candles.size.toFloat())
        binding.candlestickChart.invalidate()
    }

    // ── OHLC bar ───────────────────────────────────────────────────────────────

    private fun updateOhlcBar(candles: List<Candlestick>) {
        val b = _binding ?: return
        val c = candles.lastOrNull() ?: return
        b.tvOhlcOpen.text  = String.format(Locale.US, "%.5f", c.open)
        b.tvOhlcHigh.text  = String.format(Locale.US, "%.5f", c.high)
        b.tvOhlcLow.text   = String.format(Locale.US, "%.5f", c.low)
        b.tvOhlcClose.text = String.format(Locale.US, "%.5f", c.close)
        val isUp = c.close >= c.open
        b.tvOhlcClose.setTextColor(Color.parseColor(if (isUp) "#26A69A" else "#EF5350"))
        b.tvOhlcHigh.setTextColor(Color.parseColor("#26A69A"))
        b.tvOhlcLow.setTextColor(Color.parseColor("#EF5350"))
    }

    // ── Trade markers ──────────────────────────────────────────────────────────
    // Show only the LATEST open trade entry line + last 2 closed trades

    private fun drawTradeMarkers(trades: List<Trade>) {
        val chart = binding.candlestickChart
        chart.axisLeft.removeAllLimitLines()

        // Show: all open trades + last 3 closed trades only
        val openTrades   = trades.filter { it.status == "OPEN" }
        val closedTrades = trades.filter { it.status == "CLOSED" }.take(3)

        (openTrades + closedTrades).forEach { t ->
            val isBuy = t.type == "BUY"
            val entryColor = if (isBuy) "#26A69A" else "#EF5350"

            val entryLine = LimitLine(t.entryPrice.toFloat()).apply {
                lineWidth  = if (t.status == "OPEN") 1.2f else 0.7f
                lineColor  = Color.parseColor(entryColor)
                enableDashedLine(if (t.status == "OPEN") 12f else 6f, 4f, 0f)
                textColor  = Color.parseColor(entryColor)
                textSize   = 8f
                label      = "${t.type} ${String.format(Locale.US, "%.5f", t.entryPrice)}"
                labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
            }
            chart.axisLeft.addLimitLine(entryLine)

            if (t.status == "CLOSED" && t.exitPrice > 0) {
                val exitLine = LimitLine(t.exitPrice.toFloat()).apply {
                    lineWidth  = 0.6f
                    lineColor  = Color.parseColor("#607D8B")
                    enableDashedLine(4f, 4f, 0f)
                    textColor  = Color.parseColor("#607D8B")
                    textSize   = 7f
                    label      = "Exit ${String.format(Locale.US, "%.5f", t.exitPrice)}"
                    labelPosition = LimitLine.LimitLabelPosition.RIGHT_BOTTOM
                }
                chart.axisLeft.addLimitLine(exitLine)
            }
        }
        chart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }
}
