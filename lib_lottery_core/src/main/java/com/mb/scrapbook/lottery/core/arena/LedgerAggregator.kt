package com.mb.scrapbook.lottery.core.arena

import com.mb.scrapbook.lottery.core.domain.Draw
import com.mb.scrapbook.lottery.core.domain.Hit
import com.mb.scrapbook.lottery.core.domain.LedgerEntry
import com.mb.scrapbook.lottery.core.replay.NullDistribution
import com.mb.scrapbook.lottery.core.replay.PValue

/**
 * 账本聚合与 live 判决(设计 R5):PeriodResult 归档 → LedgerEntry;
 * live < 30 期「样本不足,暂不判决」;≥ 30 期 horizon-matched null(同长度、同每期注数、含弃权结构)判决。
 */
object LedgerAggregator {

    const val MIN_LIVE_PERIODS = 30

    fun aggregate(results: List<PeriodResult>): Map<String, LedgerEntry> {
        val byStrategy = results.groupBy { it.strategyId }
        return byStrategy.map { (id, rs) ->
            val dist = LinkedHashMap<Hit, Int>()
            rs.forEach { r -> r.settlement.prizes.forEach { dist[Hit(it.reds, it.blueHit)] = (dist[Hit(it.reds, it.blueHit)] ?: 0) + 1 } }
            LedgerEntry(
                strategyId = id,
                totalStakeYuan = rs.sumOf { it.settlement.stakeYuan },
                totalWinningsYuan = rs.sumOf { it.settlement.winningsYuan },
                hitDistribution = dist,
            )
        }.associateBy { it.strategyId }
    }

    /**
     * live 判决(R5):每选手独立数已结算期数;< 30 → INSUFFICIENT_SAMPLE(只报 ROI/命中分布);
     * ≥ 30 且有实际投入 → horizon-matched null 判决,弃权期计 0 注(掩码入 null 缓存键)。
     * α/K 的 K = 同表实际判决人数(样本不足者不占 K,未被判决)。
     */
    fun judgeLive(
        results: List<PeriodResult>,
        settledDraws: List<Draw>,
        nulls: NullDistribution = NullDistribution(),
        alpha: Double = 0.05,
    ): Map<String, LedgerEntry> {
        val periodIndex = settledDraws.withIndex().associate { (i, d) -> d.period to i }
        val ledger = aggregate(results)
        val ready = mutableListOf<LedgerEntry>()
        val nullMap = HashMap<String, DoubleArray>()
        for ((id, rs) in results.groupBy { it.strategyId }) {
            val entry = ledger.getValue(id)
            val settledCount = rs.size
            val hasStake = entry.totalStakeYuan > 0
            if (settledCount < MIN_LIVE_PERIODS || !hasStake) continue
            // horizon-matched:同长度、同每期注数、弃权期(0 注)不投不计
            val active = BooleanArray(settledDraws.size) { false }
            rs.forEach { r -> periodIndex[r.period]?.let { active[it] = r.tickets.isNotEmpty() } }
            val ticketsPerPeriod = rs.maxOf { it.tickets.size }.coerceAtLeast(1)
            nullMap[id] = nulls.roi(settledDraws, ticketsPerPeriod, active.toList())
            ready += entry
        }
        val judged = if (ready.isEmpty()) emptyList() else PValue.judge(ready, nullMap, alpha)
        val insufficient = ledger.values.filter { e -> e.strategyId !in judged.map { it.strategyId }.toSet() }
        return (judged + insufficient).associateBy { it.strategyId }
    }
}
