package com.forexbot.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.forexbot.databinding.FragmentMarketBinding
import com.forexbot.viewmodel.BotViewModel
import java.util.Locale

class MarketFragment : Fragment() {

    private var _binding: FragmentMarketBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BotViewModel by activityViewModels()

    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            viewModel.fetchStatus()
            viewModel.fetchLivePrice()
            handler.postDelayed(this, 4000)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?) =
        FragmentMarketBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── Start Bot button with SL/TP ────────────────────────────────────────
        binding.btnStartStop.setOnClickListener {
            val running = viewModel.status.value?.running ?: false
            if (running) {
                viewModel.stopBot()
            } else {
                val tp = binding.etTargetProfit.text.toString().toDoubleOrNull() ?: 20.0
                val sl = binding.etMaxLoss.text.toString().toDoubleOrNull() ?: 7.0
                if (tp <= 0 || sl <= 0) {
                    Toast.makeText(context, "Enter valid Target and SL values", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                viewModel.startBot(tp, sl)
            }
        }

        // ── Navigate to chart on EUR/USD row tap ──────────────────────────────
        binding.rowEurusd.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.forexbot.R.id.nav_host_fragment, ChartFragment())
                .addToBackStack(null)
                .commit()
        }

        // ── Observe status ────────────────────────────────────────────────────
        viewModel.status.observe(viewLifecycleOwner) { s ->
            val running = s.running
            binding.btnStartStop.text = if (running) "STOP BOT" else "START BOT"
            binding.btnStartStop.backgroundTintList = ColorStateList.valueOf(
                Color.parseColor(if (running) "#EF5350" else "#26A69A"))

            binding.tvBotStatusBadge.text = if (running) "AUTO" else "IDLE"
            binding.tvBotStatusBadge.setBackgroundColor(
                Color.parseColor(if (running) "#26A69A" else "#607D8B"))

            binding.tvBalance.text = String.format(Locale.US, "$%.2f", s.walletBalance)
            val pnl = s.totalPnL
            binding.tvPnl.text = String.format(Locale.US, "%s$%.2f", if (pnl >= 0) "+" else "", pnl)
            binding.tvPnl.setTextColor(Color.parseColor(if (pnl >= 0) "#26A69A" else "#EF5350"))

            binding.tvEmaShort.text = if (s.emaShort > 0) String.format(Locale.US, "%.5f", s.emaShort) else "—"
            binding.tvEmaLong.text  = if (s.emaLong  > 0) String.format(Locale.US, "%.5f", s.emaLong)  else "—"

            val sig = s.lastSignal.ifEmpty { "NONE" }.replace("_", " ")
            binding.tvSignal.text = sig
            binding.tvSignal.setTextColor(Color.parseColor(when {
                sig.contains("BUY") || sig.contains("GOLDEN") -> "#26A69A"
                sig.contains("SELL") || sig.contains("DEATH") -> "#EF5350"
                else -> "#FFB300"
            }))

            // Show active SL/TP from server
            if (s.running) {
                binding.tvSlTpStatus.text = "TP: $${s.targetProfit}  |  SL: $${s.maxLoss}"
                binding.tvSlTpStatus.visibility = View.VISIBLE
            } else {
                binding.tvSlTpStatus.visibility = View.GONE
            }

            // Disable inputs while running
            binding.etTargetProfit.isEnabled = !running
            binding.etMaxLoss.isEnabled = !running
            binding.etTargetProfit.alpha = if (running) 0.5f else 1.0f
            binding.etMaxLoss.alpha = if (running) 0.5f else 1.0f
        }

        // ── Live bid/ask ─────────────────────────────────────────────────────
        viewModel.ask.observe(viewLifecycleOwner) { binding.tvRowAsk.text = String.format(Locale.US, "%.5f", it) }
        viewModel.bid.observe(viewLifecycleOwner) { binding.tvRowBid.text = String.format(Locale.US, "%.5f", it) }
        viewModel.priceUp.observe(viewLifecycleOwner) { up ->
            val c = Color.parseColor(if (up) "#26A69A" else "#EF5350")
            binding.tvRowAsk.setTextColor(c)
            binding.tvRowBid.setTextColor(c)
            binding.tvChangeIndicator.text = if (up) "▲" else "▼"
            binding.tvChangeIndicator.setTextColor(c)
        }
    }

    override fun onResume() { super.onResume(); handler.post(refreshRunnable) }
    override fun onPause()  { super.onPause();  handler.removeCallbacks(refreshRunnable) }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
