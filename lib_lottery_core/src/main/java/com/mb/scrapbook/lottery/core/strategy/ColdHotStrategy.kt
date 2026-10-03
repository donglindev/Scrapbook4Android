package com.mb.scrapbook.lottery.core.strategy

import com.mb.scrapbook.lottery.core.domain.Draw

/**
 * 冷号猎手(设计 R3,cold_hot 移植):近 30 期频率最低的 6 红 + 1 蓝(平局号码升序)。
 * 未移植 lotterylab 分区/热/温模式——竞技场只需冷号一种。
 */
class ColdHotStrategy : Strategy {
    override val id = "cold_hot"
    override val displayName = "冷号猎手"
    override val requiresModel = false

    override fun pick(ctx: PickContext) =
        if (ctx.history.size < Analysis.MIN_HISTORY) {
            uniformPlan(ctx, id)
        } else {
            val recent = Analysis.recentFrequency(ctx.history)
            val reds = coldest(recent.red, Draw.RED_COUNT)
            singlePlan(id, reds, coldest(recent.blue, 1).first())
        }

    /** 权重最低优先,平局号码升序。 */
    private fun coldest(counts: Map<Int, Int>, n: Int): List<Int> =
        counts.entries
            .sortedWith(compareBy<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
            .take(n)
            .map { it.key }
            .sorted()
}
