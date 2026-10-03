package com.mb.scrapbook.lottery.ui.ledger

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mb.scrapbook.lottery.R
import com.mb.scrapbook.lottery.databinding.ItemLedgerRowBinding
import com.mb.scrapbook.lottery.ui.table.JudgedRow

/** 账本行:判决徽章(视觉锚)+ ROI + p 值 + 7 段命中分布。 */
class LedgerRowAdapter : RecyclerView.Adapter<LedgerRowAdapter.Holder>() {

    private val rows = mutableListOf<JudgedRow>()

    fun submit(new: List<JudgedRow>) {
        rows.clear()
        rows += new
        notifyDataSetChanged()
    }

    class Holder(val binding: ItemLedgerRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemLedgerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = rows[position]
        val context = holder.binding.root.context
        with(holder.binding) {
            ledgerName.text = row.displayName
            ledgerBadge.text = row.badgeText
            ledgerBadge.setTextColor(ContextCompat.getColor(context, row.badgeColor))
            ledgerRoi.text = "${"%.1f".format(row.roi * 100)}%"
            ledgerRoi.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (row.roi >= 0) R.color.lottery_green else R.color.lottery_red,
                )
            )
            ledgerP.text = "p=${row.pFormatted}"
            renderHitBar(ledgerHitBar, row)
            root.contentDescription = row.contentDescription() // D16
        }
    }

    /** 7 段命中条(6红..0红),宽度 ∝ 计数;TD3 升级为 lib_views 式自定义 View。 */
    private fun renderHitBar(bar: LinearLayout, row: JudgedRow) {
        bar.removeAllViews()
        val context = bar.context
        val total = row.redHitCounts.sum().coerceAtLeast(1)
        row.redHitCounts.forEach { count ->
            val weight = (count.toFloat() / total).coerceAtLeast(0.02f)
            val segment = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, weight)
                gravity = Gravity.CENTER
                setBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        if (count > 0) R.color.lottery_hit_segment else R.color.lottery_surface
                    )
                )
                (layoutParams as LinearLayout.LayoutParams).marginEnd = 2
            }
            bar.addView(segment)
        }
    }
}
