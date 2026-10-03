package com.mb.scrapbook.lottery.core.strategy

import com.mb.scrapbook.lottery.core.domain.BetPlan
import com.mb.scrapbook.lottery.core.domain.BetScheme
import com.mb.scrapbook.lottery.core.domain.Draw
import kotlin.random.Random

/** 采样工具:全部确定性依赖传入 rng,无隐藏随机源。 */
object Sampling {

    /** 均匀采 count 个互异红球(升序)。 */
    fun uniformReds(rng: Random, count: Int = Draw.RED_COUNT): List<Int> =
        (1..Draw.RED_MAX).shuffled(rng).take(count).sorted()

    /** 均匀采 1 蓝。 */
    fun uniformBlue(rng: Random): Int = rng.nextInt(Draw.BLUE_MAX) + 1

    /**
     * 权重无放回采样(每轮按剩余权重抽一个)。全零权重时均匀回退(防御;频率场景冷启动已在策略层挡掉)。
     * 遍历按键升序,保证同权重布局下同 rng 序列同结果。
     */
    fun weightedSample(rng: Random, weights: Map<Int, Int>, count: Int): List<Int> {
        require(count <= weights.size) { "采样数 $count 超过候选数 ${weights.size}" }
        val remaining = weights.toMutableMap()
        val picked = ArrayList<Int>(count)
        repeat(count) {
            val keys = remaining.keys.sorted()
            val total = remaining.values.sum()
            val chosen = if (total <= 0) {
                keys[rng.nextInt(keys.size)]
            } else {
                var r = rng.nextLong(total.toLong())
                var hit = keys.last()
                for (k in keys) {
                    r -= remaining.getValue(k)
                    if (r < 0) {
                        hit = k
                        break
                    }
                }
                hit
            }
            remaining.remove(chosen)
            picked += chosen
        }
        return picked.sorted()
    }
}

/** 单注计划。generatedAt=0:回放确定性优先,真实时间戳由 arena 层(T5)落 PeriodResult。 */
internal fun singlePlan(strategyId: String, reds: List<Int>, blue: Int): BetPlan =
    BetPlan(listOf(BetScheme(reds, blue)), 0L, mapOf("strategy" to strategyId))

/** 冷启动回退(设计 R5):统计策略历史 < 30 期时的均匀单注。 */
internal fun uniformPlan(ctx: PickContext, strategyId: String): BetPlan =
    singlePlan(strategyId, Sampling.uniformReds(ctx.rng), Sampling.uniformBlue(ctx.rng))
