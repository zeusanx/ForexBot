package com.forexbot.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.forexbot.R
import com.forexbot.api.SessionManager
import com.forexbot.databinding.ActivityMainBinding
import com.forexbot.viewmodel.BotViewModel
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: BotViewModel

    private val handler = Handler(Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        override fun run() {
            viewModel.fetchStatusAndTick()
            handler.postDelayed(this, 3000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[BotViewModel::class.java]

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        binding.bottomNavigation.setupWithNavController(navHostFragment.navController)

        // Set username in header
        binding.tvUserName.text = SessionManager.getName(this)

        // Live price is updated by candle close via fetchChartData() in MarketFragment
        viewModel.ask.observe(this) { ask ->
            binding.tvAsk.text = String.format(Locale.US, "%.5f", ask)
        }
        viewModel.bid.observe(this) { bid ->
            binding.tvBid.text = String.format(Locale.US, "%.5f", bid)
        }
        viewModel.priceUp.observe(this) { up ->
            val color = if (up) Color.parseColor("#26A69A") else Color.parseColor("#EF5350")
            binding.tvAsk.setTextColor(color)
            binding.tvBid.setTextColor(color)
            binding.tvSpread.setTextColor(color)
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(tickRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tickRunnable)
    }
}
