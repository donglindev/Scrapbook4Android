package com.mb.scrapbook.lottery.ui.table

import com.mb.scrapbook.lottery.R
import com.mb.scrapbook.lottery.core.domain.LedgerEntry
import com.mb.scrapbook.lottery.core.domain.Verdict
import com.mb.scrapbook.lottery.core.replay.PValue

/** 判决表 UI 行:竞技场卡片与诚实账本共用。 */
data class JudgedRow(
    val strategyId: String,
    val displayName: String,
    val verdict: Verdict,
    val roi: Double,
    val pFormatted: String,
    val totalStakeYuan: Long,
    val totalWinningsYuan: Long,
    /** 7 段命中计数,红命中 6..0(蓝折叠;contentDescription 展开)。 */
    val redHitCounts: List<Int>,
) {
    val badgeText: String
        get() = when (verdict) {
            Verdict.NOT_SIGNIFICANT -> "✕ 与随机无差异"
            Verdict.SIGNIFICANTLY_WORSE -> "▼ 显著更差"
            Verdict.SIGNIFICANTLY_BETTER -> "⚠ 数据警报:显著更好"
            Verdict.INSUFFICIENT_SAMPLE -> "… 样本不足"
        }

    /** 徽章色(R.color 资源 id)。 */
    val badgeColor: Int
        get() = when (verdict) {
            Verdict.NOT_SIGNIFICANT -> R.color.lottery_badge_neutral
            Verdict.SIGNIFICANTLY_WORSE -> R.color.lottery_badge_worse
            Verdict.SIGNIFICANTLY_BETTER -> R.color.lottery_badge_alert
            Verdict.INSUFFICIENT_SAMPLE -> R.color.lottery_badge_insufficient
        }

    /** D16:语义 contentDescription(TalkBack)。 */
    fun contentDescription(): String =
        "$displayName,${badgeText.removePrefix("✕ ").removePrefix("▼ ").removePrefix("⚠ ").removePrefix("… ")},ROI ${"%.1f".format(roi * 100)}%" +
            ",命中分布 6红${redHitCounts[0]}次,5红${redHitCounts[1]}次,4红${redHitCounts[2]}次,3红${redHitCounts[3]}次,2红${redHitCounts[4]}次,1红${redHitCounts[5]}次,0红${redHitCounts[6]}次"

    companion object {
        fun of(entry: LedgerEntry, displayName: String): JudgedRow = JudgedRow(
            strategyId = entry.strategyId,
            displayName = displayName,
            verdict = entry.verdict,
            roi = entry.roi,
            pFormatted = entry.pValue?.let { PValue.format(it) } ?: "—",
            totalStakeYuan = entry.totalStakeYuan,
            totalWinningsYuan = entry.totalWinningsYuan,
            redHitCounts = (0..6).map { red ->
                entry.hitDistribution.count { (hit, _) -> hit.reds == red }
            }, // 索引 0=6红 … 6=0红
        )
    }
}
