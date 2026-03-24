package com.forexbot.adapter

import android.graphics.Color
import android.view.*
import androidx.recyclerview.widget.RecyclerView
import com.forexbot.databinding.ItemTradeBinding
import com.forexbot.model.Trade
import java.util.Locale

class TradeAdapter : RecyclerView.Adapter<TradeAdapter.VH>() {
    private val trades = mutableListOf<Trade>()
    fun setTrades(list: List<Trade>) { trades.clear(); trades.addAll(list); notifyDataSetChanged() }
    override fun getItemCount() = trades.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemTradeBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(trades[position])

    inner class VH(private val b: ItemTradeBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(t: Trade) {
            b.tvTradeId.text = "#${t.id}"
            b.tvPair.text = t.currencyPair
            val isBuy = t.type == "BUY"
            b.tvType.text = t.type
            b.tvType.setBackgroundColor(Color.parseColor(if (isBuy) "#26A69A" else "#EF5350"))
            val isOpen = t.status == "OPEN"
            b.tvStatus.text = if (isOpen) "OPEN" else "CLOSED"
            b.tvStatus.setTextColor(Color.parseColor(if (isOpen) "#FFB300" else "#8B949E"))
            b.tvEntryPrice.text = String.format(Locale.US, "%.5f", t.entryPrice)
            b.tvExitPrice.text  = if (t.exitPrice > 0) String.format(Locale.US, "%.5f", t.exitPrice) else "—"
            if (!isOpen) {
                val pnl = t.profitLoss
                b.tvPnl.text = String.format(Locale.US, "%s$%.4f", if (pnl>=0) "+" else "", pnl)
                b.tvPnl.setTextColor(Color.parseColor(if (pnl>=0) "#26A69A" else "#EF5350"))
            } else { b.tvPnl.text = "—"; b.tvPnl.setTextColor(Color.parseColor("#8B949E")) }
            b.tvLotSize.text = "${t.lotSize}"
            b.tvOpenTime.text = t.openTime.let { if (it.length >= 16) it.substring(0,16).replace("T"," ") else it }
            b.tvSignal.text = t.signal.ifEmpty { "—" }
        }
    }
}
