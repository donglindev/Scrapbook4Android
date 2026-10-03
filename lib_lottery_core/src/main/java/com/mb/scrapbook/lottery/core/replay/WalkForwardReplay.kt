package com.mb.scrapbook.lottery.core.replay

import com.mb.scrapbook.lottery.core.domain.Draw
import com.mb.scrapbook.lottery.core.domain.Hit
import com.mb.scrapbook.lottery.core.domain.LedgerEntry
import com.mb.scrapbook.lottery.core.domain.PrizeTable
import com.mb.scrapbook.lottery.core.strategy.PickContext
import com.mb.scrapbook.lottery.core.strategy.Strategy
import kotlin.random.Random

/**
 * 走前回放引擎(设计 R5):期 t 策略只见 [0,t),出票 → 兑奖 → 入账。
 * 固定种子确定性:同种子同账本(快照测试锁定)。时间戳语义:策略内 generatedAt=0,
 * 真实 pickAt/enteredAt 由 arena 层(T5)落 PeriodResult。
 * requiresModel 选手不传入(缺席记录是 SeasonRunner 的职责,本引擎只跑在场的纯 JVM 选手)。
 */
class WalkForwardReplay(
    private val strategies: List<Strategy>,
    private val seed: Long = DEFAULT_SEED,
    /** PixelSeed 种子照片字节(R6:回放用打包资产照片;资产未入库前默认空,仍确定性)。 */
    private val seedBytes: ByteArray = ByteArray(0),
) {

    fun run(history: List<Draw>): ReplayResult {
        val rngs: Map<String, Random> = strategies.associate { it.id to Random(mix(seed, it.id)) }
        val stake = HashMap<String, Long>()
        val winnings = HashMap<String, Long>()
        val tickets = HashMap<String, Long>()
        val hitDist = HashMap<String, LinkedHashMap<Hit, Int>>()
        for (s in strategies) {
            stake[s.id] = 0L
            winnings[s.id] = 0L
            tickets[s.id] = 0L
            hitDist[s.id] = LinkedHashMap()
        }

        for (t in history.indices) {
            val past = history.subList(0, t)
            val draw = history[t]
            for (s in strategies) {
                val plan = s.pick(PickContext(draw.period, past, rngs.getValue(s.id), seedBytes))
                stake[s.id] = stake.getValue(s.id) + plan.totalCost
                tickets[s.id] = tickets.getValue(s.id) + plan.schemes.size
                val dist = hitDist.getValue(s.id)
                for (scheme in plan.schemes) {
                    val hit = scheme.hit(draw)
                    dist[hit] = (dist[hit] ?: 0) + 1
                    winnings[s.id] = winnings.getValue(s.id) + (PrizeTable.prizeFor(hit)?.amountYuan ?: 0L)
                }
            }
        }

        val periods = history.size
        val ledgers = strategies.associate { s ->
            s.id to LedgerEntry(
                strategyId = s.id,
                totalStakeYuan = stake.getValue(s.id),
                totalWinningsYuan = winnings.getValue(s.id),
                hitDistribution = hitDist.getValue(s.id),
            )
        }
        val ticketsPerPeriod = strategies.associate { s ->
            // 注数按期平均取整;竞技场阵容下为精确值(1 或 46)
            s.id to (tickets.getValue(s.id) / periods).toInt()
        }
        return ReplayResult(ledgers, ticketsPerPeriod, periods, seed)
    }

    companion object {
        const val DEFAULT_SEED = 2026L

        /** 策略专属随机流:seed × 策略 id 混合,同种子同流。 */
        fun mix(seed: Long, strategyId: String): Long = "$seed|$strategyId".hashCode().toLong()
    }
}

/** 回放产出:选手账本 + 每期注数(null 匹配键用)。 */
data class ReplayResult(
    val ledgers: Map<String, LedgerEntry>,
    val ticketsPerPeriod: Map<String, Int>,
    val periods: Int,
    val seed: Long,
)
