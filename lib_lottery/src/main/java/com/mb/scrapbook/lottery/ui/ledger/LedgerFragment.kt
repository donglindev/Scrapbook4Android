package com.mb.scrapbook.lottery.ui.ledger

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mb.scrapbook.lib.base.mvvm.view.BaseFragment
import com.mb.scrapbook.lottery.R
import com.mb.scrapbook.lottery.core.arena.LedgerAggregator
import com.mb.scrapbook.lottery.core.domain.Verdict
import com.mb.scrapbook.lottery.data.SeasonStore
import com.mb.scrapbook.lottery.databinding.FragmentLedgerBinding
import com.mb.scrapbook.lottery.strategy.Personas
import com.mb.scrapbook.lottery.ui.table.TableViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 诚实账本(D5=A):live 赛季行 + 回放判决表 + 回放缺席行 + 免责声明常驻底部(R9)。 */
class LedgerFragment : BaseFragment() {

    private lateinit var binding: FragmentLedgerBinding

    override fun getLayoutId(): Int = R.layout.fragment_ledger

    override fun onInitView(layout: View) {
        binding = DataBindingUtil.bind(layout) ?: return
        binding.ledgerList.layoutManager = LinearLayoutManager(context)
        binding.ledgerList.adapter = LedgerRowAdapter()
    }

    override fun onInitData() {
        ViewModelProvider(requireActivity()).get(TableViewModel::class.java).rows.observe(this) { rows ->
            (binding.ledgerList.adapter as? LedgerRowAdapter)?.submit(rows.orEmpty())
        }
    }

    override fun onResume() {
        super.onResume()
        renderLiveLedger()
    }

    /** live 账本(D5):空态 CTA;< 30 期「样本不足」;模型选手回放缺席灰行。 */
    private fun renderLiveLedger() {
        val container = binding.liveContainer
        container.removeAllViews()
        lifecycleScope.launch(Dispatchers.Default) {
            val store = SeasonStore(requireContext().applicationContext, lifecycleScope)
            store.load { com.mb.scrapbook.lottery.core.data.DrawRepository().load() }
            val rows = if (store.archive.isEmpty()) {
                listOf("赛季刚开始,第一晚开奖后出账 → 去开奖夜")
            } else {
                val judged = LedgerAggregator.judgeLive(store.archive, store.settledDraws)
                judged.values.sortedByDescending { it.roi }.map { e ->
                    val badge = when (e.verdict) {
                        Verdict.NOT_SIGNIFICANT -> "✕ 与随机无差异"
                        Verdict.SIGNIFICANTLY_WORSE -> "▼ 显著更差"
                        Verdict.SIGNIFICANTLY_BETTER -> "⚠ 数据警报"
                        Verdict.INSUFFICIENT_SAMPLE -> "… 样本不足(${store.settledDraws.size}/30)"
                    }
                    "${e.strategyId} · ROI ${"%.1f".format(e.roi * 100)}% · ¥${e.totalStakeYuan} 投入 · $badge"
                }
            }
            val absent = Personas.ALL.map { "── ${it.displayName}:回放缺席(requiresModel,仅 live) ──" }
            withContext(Dispatchers.Main) {
                (rows + absent).forEach { text ->
                    container.addView(
                        TextView(requireContext()).apply {
                            this.text = text
                            setPadding(0, 8, 0, 8)
                            textSize = 13f
                            setTextColor(
                                ContextCompat.getColor(
                                    context,
                                    if (text.startsWith("──")) R.color.lottery_badge_insufficient else R.color.lottery_text_primary
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}
