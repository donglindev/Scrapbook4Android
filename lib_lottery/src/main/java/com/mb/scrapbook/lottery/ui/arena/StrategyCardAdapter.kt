package com.mb.scrapbook.lottery.ui.arena

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mb.scrapbook.lottery.core.strategy.Strategy
import com.mb.scrapbook.lottery.databinding.ItemStrategyCardBinding
import com.mb.scrapbook.lottery.ui.table.JudgedRow

/** 竞技场卡片:行1 名+徽章;行2 ROI 大数字(盈绿/亏红/对照灰);行3 投注/中奖明细。 */
class StrategyCardAdapter : RecyclerView.Adapter<StrategyCardAdapter.Holder>() {

    private val rows = mutableListOf<JudgedRow>()

    fun submit(new: List<JudgedRow>) {
        rows.clear()
        rows += new
        notifyDataSetChanged()
    }

    class Holder(val binding: ItemStrategyCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemStrategyCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = rows[position]
        val context = holder.binding.root.context
        binding(holder, row, context)
    }

    private fun binding(holder: Holder, row: JudgedRow, context: android.content.Context) {
        val b = holder.binding
        b.cardName.text = row.displayName
        b.cardBadge.text = row.badgeText
        b.cardBadge.setTextColor(ContextCompat.getColor(context, row.badgeColor))
        b.cardRoi.text = "${"%.1f".format(row.roi * 100)}%"
        b.cardRoi.setTextColor(
            ContextCompat.getColor(
                context,
                when {
                    row.strategyId == "random" -> com.mb.scrapbook.lottery.R.color.lottery_text_secondary // 对照组灰(D4)
                    row.roi >= 0 -> com.mb.scrapbook.lottery.R.color.lottery_green
                    else -> com.mb.scrapbook.lottery.R.color.lottery_red
                },
            )
        )
        b.cardDetail.text = "投入 ¥${row.totalStakeYuan} · 中奖 ¥${row.totalWinningsYuan} · p=${row.pFormatted}"
        b.root.contentDescription = row.contentDescription() // D16
    }
}
