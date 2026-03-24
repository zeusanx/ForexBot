package com.forexbot.ui

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.forexbot.adapter.TradeAdapter
import com.forexbot.databinding.FragmentTradesBinding
import com.forexbot.viewmodel.BotViewModel

class TradesFragment : Fragment() {
    private var _binding: FragmentTradesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BotViewModel by activityViewModels()
    private val adapter = TradeAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?) =
        FragmentTradesBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvTrades.layoutManager = LinearLayoutManager(context)
        binding.rvTrades.adapter = adapter
        binding.swipeRefresh.setColorSchemeColors(0xFF26A69A.toInt(), 0xFF2196F3.toInt())
        binding.swipeRefresh.setOnRefreshListener { viewModel.fetchTrades() }
        viewModel.trades.observe(viewLifecycleOwner) { trades ->
            binding.swipeRefresh.isRefreshing = false
            if (trades.isNullOrEmpty()) { binding.tvNoTrades.visibility = View.VISIBLE; binding.rvTrades.visibility = View.GONE }
            else { binding.tvNoTrades.visibility = View.GONE; binding.rvTrades.visibility = View.VISIBLE; adapter.setTrades(trades) }
        }
        viewModel.fetchTrades()
    }
    override fun onResume() { super.onResume(); viewModel.fetchTrades() }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
