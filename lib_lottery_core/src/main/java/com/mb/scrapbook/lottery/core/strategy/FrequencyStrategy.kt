package com.mb.scrapbook.lottery.core.strategy

import com.mb.scrapbook.lottery.core.domain.Draw
import com.mb.scrapbook.lottery.core.domain.NumberPool

/**
 * 频率派(设计 R3):全历史频率加权采样出 1 注;冷启动(history < 30)回退均匀采样(R5)。
 * 另产确定性 top-N 号码池供旋转矩阵缩水(设计 R4:12 红 + 1 蓝)。
 * 未移植 lotterylab 分区 Zone 预设——竞技场只需加权采样与全局 top-N 池。
 */
class FrequencyStrategy : Strategy {
    override val id = "freq"
    override val displayName = "频率派"
    override val requiresModel = false

    override fun pick(ctx: PickContext) =
        if (ctx.history.size < Analysis.MIN_HISTORY) {
            uniformPlan(ctx, id)
        } else {
            val f = Analysis.frequency(ctx.history)
            val reds = Sampling.weightedSample(ctx.rng, f.red, Draw.RED_COUNT)
            singlePlan(id, reds, Sampling.weightedSample(ctx.rng, f.blue, 1).first())
        }

    /** 频率 top-N 号码池(平局号码升序,确定性)。矩阵消费(设计 R4);红池至少 6。 */
    fun pool(ctx: PickContext, redPoolSize: Int = 12, bluePoolSize: Int = 1): NumberPool {
        require(redPoolSize >= Draw.RED_COUNT) { "红池至少 ${Draw.RED_COUNT} 个: $redPoolSize" }
        if (ctx.history.size < Analysis.MIN_HISTORY) {
            // 冷启动:经 ctx.rng 均匀采样,回放固定种子下同样可复现
            return NumberPool(
                (1..Draw.RED_MAX).shuffled(ctx.rng).take(redPoolSize).toSet(),
                setOf(Sampling.uniformBlue(ctx.rng)),
                id,
            )
        }
        val f = Analysis.frequency(ctx.history)
        val reds = topByWeight(f.red, redPoolSize)
        val blues = topByWeight(f.blue, bluePoolSize)
        return NumberPool(reds.toSet(), blues.toSet(), id)
    }

    private fun topByWeight(counts: Map<Int, Int>, n: Int): List<Int> =
        counts.entries
            .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
            .take(n)
            .map { it.key }
}
