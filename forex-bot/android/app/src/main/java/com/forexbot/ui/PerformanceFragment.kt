package com.forexbot.ui

import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.forexbot.databinding.FragmentPerformanceBinding
import com.forexbot.model.Performance
import com.forexbot.viewmodel.BotViewModel
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.PercentFormatter
import java.util.Locale

class PerformanceFragment : Fragment() {
    private var _binding: FragmentPerformanceBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BotViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?) =
        FragmentPerformanceBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.pieChart.apply {
            setBackgroundColor(Color.parseColor("#131722"))
            description.isEnabled = false
            setHoleColor(Color.parseColor("#131722"))
            holeRadius = 55f; transparentCircleRadius = 60f
            setCenterTextColor(Color.WHITE); setCenterTextSize(14f)
            setUsePercentValues(true)
            legend.textColor = Color.parseColor("#8B949E")
        }
        viewModel.performance.observe(viewLifecycleOwner) { renderPerformance(it) }
        binding.btnRefreshPerf.setOnClickListener { viewModel.fetchPerformance() }
        viewModel.fetchPerformance()
    }
    override fun onResume() { super.onResume(); viewModel.fetchPerformance() }

    private fun renderPerformance(p: Performance) {
        val b = _binding ?: return
        b.tvCurrentBalance.text = String.format(Locale.US, "$%.2f", p.currentBalance)
        b.tvInitialBalance.text = String.format(Locale.US, "$%.2f", p.initialBalance)
        b.tvTotalPnl.text = String.format(Locale.US, "%s$%.4f", if (p.totalPnL>=0) "+" else "", p.totalPnL)
        b.tvTotalPnl.setTextColor(Color.parseColor(if (p.totalPnL>=0) "#26A69A" else "#EF5350"))
        b.tvReturn.text = String.format(Locale.US, "%s%.2f%%", if (p.returnPercent>=0) "+" else "", p.returnPercent)
        b.tvReturn.setTextColor(Color.parseColor(if (p.returnPercent>=0) "#26A69A" else "#EF5350"))
        b.tvTotalTrades.text = p.totalTrades.toString()
        b.tvWinningTrades.text = p.winningTrades.toString()
        b.tvLosingTrades.text  = p.losingTrades.toString()
        b.tvWinRate.text = String.format(Locale.US, "%.1f%%", p.winRate)
        b.tvWinRate.setTextColor(Color.parseColor(if (p.winRate>=50) "#26A69A" else "#EF5350"))
        val entries = mutableListOf<PieEntry>()
        if (p.winningTrades>0) entries.add(PieEntry(p.winningTrades.toFloat(), "Wins"))
        if (p.losingTrades>0)  entries.add(PieEntry(p.losingTrades.toFloat(), "Losses"))
        if (entries.isEmpty()) entries.add(PieEntry(1f, "No Trades"))
        val ds = PieDataSet(entries,"").apply {
            colors = listOf(Color.parseColor("#26A69A"), Color.parseColor("#EF5350"), Color.parseColor("#607D8B"))
            sliceSpace = 3f; selectionShift = 8f; valueTextColor = Color.WHITE; valueTextSize = 12f
        }
        b.pieChart.data = PieData(ds).apply { setValueFormatter(PercentFormatter(b.pieChart)) }
        b.pieChart.setCenterText(String.format(Locale.US,"%.1f%%\nWin Rate",p.winRate))
        b.pieChart.invalidate()
    }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
