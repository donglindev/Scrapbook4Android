package com.mb.scrapbook.lottery.core.replay

import com.mb.scrapbook.lottery.core.domain.Draw
import com.mb.scrapbook.lottery.core.domain.Hit
import com.mb.scrapbook.lottery.core.domain.PrizeTable
import kotlin.random.Random

/**
 * 零分布(设计 R5,工程评审 C2):与选手**同注量、同期数**的纯随机赛季构造
 * stake-normalized ROI 的经验分布,作双侧 p 值的比较基准。
 * 共享缓存:同(每期注数, 期数)的零分布数学上同分布 → 1 注/期策略共享同一份(省约 5 倍计算);
 * matrix(46 注/期)独立构造。含弃权结构的 live horizon null 后续扩展键(R5)。
 */
class NullDistribution(
    private val runs: Int = DEFAULT_RUNS,
    private val seed: Long = DEFAULT_SEED,
) {

    private val cache = HashMap<NullKey, DoubleArray>()

    /** 排序后的 ROI 零分布。同键共享同一实例(C2)。 */
    fun roi(history: List<Draw>, ticketsPerPeriod: Int): DoubleArray =
        cache.getOrPut(NullKey(ticketsPerPeriod, history.size)) { compute(history, ticketsPerPeriod) }

    private fun compute(history: List<Draw>, ticketsPerPeriod: Int): DoubleArray {
        require(history.isNotEmpty()) { "历史为空,无法构造零分布" }
        require(ticketsPerPeriod >= 1) { "每期注数至少 1: $ticketsPerPeriod" }
        val periods = history.size
        val totalStake = ticketsPerPeriod.toLong() * 2L * periods // 2 = BetPlan.TICKET_PRICE_YUAN

        // 每期预计算红球命中表,全部 run×ticket 复用
        val redPresent = Array(periods) { p -> BooleanArray(DRAW_RED_MAX + 1).also { b -> history[p].reds.forEach { b[it] = true } } }
        val blues = IntArray(periods) { history[it].blue }

        // 键派生随机流:不同键独立,同键确定性
        val rng = Random(seed xor (ticketsPerPeriod.toLong() * 1_000_003L) xor periods.toLong())
        val scratch = IntArray(6)
        val used = BooleanArray(DRAW_RED_MAX + 1)
        val rois = DoubleArray(runs)
        for (r in 0 until runs) {
            var winnings = 0L
            for (p in 0 until periods) {
                repeat(ticketsPerPeriod) {
                    winnings += randomTicketPrize(rng, redPresent[p], blues[p], scratch, used)
                }
            }
            rois[r] = (winnings - totalStake).toDouble() / totalStake
        }
        rois.sort()
        return rois
    }

    /** 均匀采样一注并对该期兑奖(拒绝采样采 6 互异红 + 1 蓝;scratch/used 复用避免分配)。 */
    private fun randomTicketPrize(
        rng: Random,
        redPresent: BooleanArray,
        blue: Int,
        scratch: IntArray,
        used: BooleanArray,
    ): Long {
        var picked = 0
        var hits = 0
        while (picked < 6) {
            val n = rng.nextInt(DRAW_RED_MAX) + 1
            if (!used[n]) {
                used[n] = true
                scratch[picked] = n
                if (redPresent[n]) hits++
                picked++
            }
        }
        for (i in 0 until 6) used[scratch[i]] = false
        val blueHit = rng.nextInt(DRAW_BLUE_MAX) + 1 == blue
        return PrizeTable.prizeFor(Hit(hits, blueHit))?.amountYuan ?: 0L
    }

    private companion object {
        const val DRAW_RED_MAX = 33
        const val DRAW_BLUE_MAX = 16
        const val DEFAULT_RUNS = 1000
        const val DEFAULT_SEED = 4242L
    }
}

/** 共享缓存键(C2):live 弃权结构后续加入此键。 */
private data class NullKey(val ticketsPerPeriod: Int, val periods: Int)
