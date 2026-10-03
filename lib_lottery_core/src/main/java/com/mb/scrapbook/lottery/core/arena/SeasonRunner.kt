package com.mb.scrapbook.lottery.core.arena

import com.mb.scrapbook.lottery.core.domain.BetPlan
import com.mb.scrapbook.lottery.core.domain.Draw
import com.mb.scrapbook.lottery.core.domain.PrizeTable

/**
 * live 赛季单期状态机(设计 R7,D12=A settle gate):
 * recordPick / recordAbstention → settle(开奖号) → PeriodResult 列表(不可变归档)。
 * filesDir 持久化与进程重启恢复是 Android 壳(T6)的职责;本类只做核心状态机与不变量。
 */
class SeasonRunner(private val participants: Set<String>) {

    /** 已结算期(settled 后不可变,D14)。 */
    private val settledPeriods = HashSet<String>()

    /** period → strategyId → 记录。 */
    private val pending = HashMap<String, HashMap<String, Recorded>>()

    private val _archive = ArrayList<PeriodResult>()

    /** 结算归档,append-only。 */
    val archive: List<PeriodResult> get() = _archive

    fun recordPick(
        strategyId: String,
        period: String,
        plan: BetPlan,
        pickAt: Long,
        reason: String = "",
        modelMeta: Map<String, String>? = null,
    ) {
        require(strategyId in participants) { "非在场选手: $strategyId" }
        requirePeriodOpen(strategyId, period)
        pending.getOrPut(period) { HashMap() }[strategyId] = Recorded(plan, reason, pickAt, modelMeta, abstention = false)
    }

    /** 显式弃权(仅模型解析失败;计 0 注,不持票据)。 */
    fun recordAbstention(strategyId: String, period: String, reason: String, at: Long) {
        require(strategyId in participants) { "非在场选手: $strategyId" }
        requirePeriodOpen(strategyId, period)
        pending.getOrPut(period) { HashMap() }[strategyId] = Recorded(null, reason, at, null, abstention = true)
    }

    /**
     * settle gate(D12=A):在场选手全部出票或显式弃权才放行;pickAt < enteredAt 逐票断言,违例 fail loudly。
     * 结算后期不可变:重复 settle 抛 IllegalStateException(D14)。
     */
    fun settle(draw: Draw, enteredAt: Long, settledAt: Long = enteredAt): List<PeriodResult> {
        check(draw.period !in settledPeriods) { "期 ${draw.period} 已结算,不可变" }
        val records = pending[draw.period].orEmpty()
        val missing = participants - records.keys
        if (missing.isNotEmpty()) {
            throw SettleGateException("settle gate 拒绝:未完成选手 $missing", missing)
        }
        val results = participants.sorted().map { id ->
            val rec = records.getValue(id)
            check(rec.at < enteredAt) {
                "数据完整性违例: $id 在期 ${draw.period} pickAt(${rec.at}) >= enteredAt($enteredAt)"
            }
            if (rec.abstention) {
                PeriodResult(
                    strategyId = id,
                    period = draw.period,
                    reason = rec.reason,
                    abstention = true,
                    timestamps = PeriodResult.Timestamps(pickAt = rec.at, enteredAt = enteredAt, settledAt = settledAt),
                )
            } else {
                val prizes = rec.plan!!.schemes.map { scheme ->
                    val hit = scheme.hit(draw)
                    val prize = PrizeTable.prizeFor(hit)
                    PeriodResult.TicketPrize(
                        reds = hit.reds,
                        blueHit = hit.blue,
                        tier = prize?.tier?.name ?: "",
                        amountYuan = prize?.amountYuan ?: 0L,
                        assumed = prize?.assumed ?: false,
                    )
                }
                PeriodResult(
                    strategyId = id,
                    period = draw.period,
                    tickets = rec.plan.schemes,
                    reason = rec.reason,
                    settlement = PeriodResult.Settlement(
                        prizes = prizes,
                        stakeYuan = rec.plan.totalCost.toLong(),
                        winningsYuan = prizes.sumOf { it.amountYuan },
                    ),
                    timestamps = PeriodResult.Timestamps(pickAt = rec.at, enteredAt = enteredAt, settledAt = settledAt),
                    modelMeta = rec.modelMeta,
                )
            }
        }
        results.forEach { it.validate() } // 结算产物自证不变量
        settledPeriods += draw.period
        pending.remove(draw.period)
        _archive += results
        return results
    }

    private fun requirePeriodOpen(strategyId: String, period: String) {
        check(period !in settledPeriods) { "期 $period 已结算,不可变" } // 状态违例,非参数误用
        require(pending[period]?.get(strategyId) == null) { "$strategyId 在期 $period 已有记录,不可覆盖" }
    }

    private data class Recorded(
        val plan: BetPlan?,
        val reason: String,
        val at: Long,
        val modelMeta: Map<String, String>?,
        val abstention: Boolean,
    )
}

/** settle gate 拒绝(可恢复:UI 显示未完成名单后等待,D12)。 */
class SettleGateException(message: String, val missing: Collection<String>) : IllegalStateException(message)
