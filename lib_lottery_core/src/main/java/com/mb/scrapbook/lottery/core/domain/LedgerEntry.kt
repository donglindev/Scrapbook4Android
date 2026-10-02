package com.mb.scrapbook.lottery.core.domain

/** 判决徽章四态(设计 R5/D13)。SIGNIFICANTLY_BETTER 出现即「数据警报」。 */
enum class Verdict {
    /** 与随机无显著差异 */
    NOT_SIGNIFICANT,

    /** 显著更差 */
    SIGNIFICANTLY_WORSE,

    /** 显著更好(数据警报:结果异常,请检查数据与流程) */
    SIGNIFICANTLY_BETTER,

    /** 样本不足,暂不判决(live < 30 期) */
    INSUFFICIENT_SAMPLE,
}

/** 账本条目(设计 R5):一位选手的赛季聚合。聚合与判决计算在 T4/T5 落地。 */
data class LedgerEntry(
    val strategyId: String,
    val totalStakeYuan: Long,
    val totalWinningsYuan: Long,
    /** 命中分布,键覆盖 6+1 … 0+0 */
    val hitDistribution: Map<Hit, Int>,
    val pValue: Double? = null,
    val verdict: Verdict = Verdict.INSUFFICIENT_SAMPLE,
) {
    init {
        require(strategyId.isNotBlank()) { "strategyId 不能为空" }
        require(totalStakeYuan >= 0) { "累计投入不能为负" }
        require(totalWinningsYuan >= 0) { "累计中奖不能为负" }
    }

    /** ROI = (累计中奖 - 累计投入) / 累计投入 */
    val roi: Double
        get() = if (totalStakeYuan == 0L) 0.0
        else (totalWinningsYuan - totalStakeYuan).toDouble() / totalStakeYuan
}
