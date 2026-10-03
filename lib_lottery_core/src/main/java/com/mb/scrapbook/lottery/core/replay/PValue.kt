package com.mb.scrapbook.lottery.core.replay

import com.mb.scrapbook.lottery.core.domain.LedgerEntry
import com.mb.scrapbook.lottery.core.domain.Verdict

/**
 * p 值与判决(设计 R5):双侧经验分位 + 多重比较校正。
 * 校正绑定**报表阵容**:同表 K 位选手统一用 α/K(第一周 JVM 判决表 K=6 → 0.0083;
 * 实机全阵容 K=10 → 0.005)——同一数据在 JVM 与 Android 上算出的徽章必须一致。
 */
object PValue {

    /** 双侧经验 p = 2 × min(P(X≤x), P(X≥x)),封顶 1.0。sortedNull 须升序。 */
    fun twoSided(sortedNull: DoubleArray, observed: Double): Double {
        val n = sortedNull.size
        require(n > 0) { "零分布为空" }
        val le = countAtMost(sortedNull, observed)      // X ≤ x
        val ge = n - countLess(sortedNull, observed)    // X ≥ x
        val p = 2.0 * minOf(le, ge) / n
        return minOf(p, 1.0)
    }

    /** 展示格式:1000 次经验分辨率下限,低于 0.001 报 "<0.001"(R5)。 */
    fun format(p: Double): String = if (p < 0.001) "<0.001" else String.format("%.3f", p)

    /**
     * 判决表(R5):对在场 K 位选手按 α/K 统一判;显著时方向由观测相对零分布中位数定。
     * SIGNIFICANTLY_BETTER = 数据警报(任何算法打赢随机,先怀疑数据)。
     */
    fun judge(
        entries: List<LedgerEntry>,
        nulls: Map<String, DoubleArray>,
        alpha: Double = 0.05,
    ): List<LedgerEntry> {
        val threshold = alpha / entries.size
        return entries.map { e ->
            val dist = nulls.getValue(e.strategyId)
            val p = twoSided(dist, e.roi)
            val verdict = if (p < threshold) {
                if (e.roi < dist[dist.size / 2]) Verdict.SIGNIFICANTLY_WORSE else Verdict.SIGNIFICANTLY_BETTER
            } else {
                Verdict.NOT_SIGNIFICANT
            }
            e.copy(pValue = p, verdict = verdict)
        }
    }

    /** x 的个数:首个 > x 的下标(二分)。 */
    private fun countAtMost(sorted: DoubleArray, x: Double): Int {
        var lo = 0
        var hi = sorted.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (sorted[mid] <= x) lo = mid + 1 else hi = mid
        }
        return lo
    }

    /** < x 的个数:首个 ≥ x 的下标(二分)。 */
    private fun countLess(sorted: DoubleArray, x: Double): Int {
        var lo = 0
        var hi = sorted.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (sorted[mid] < x) lo = mid + 1 else hi = mid
        }
        return lo
    }
}
