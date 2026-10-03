package com.mb.scrapbook.lottery.core.strategy

import com.mb.scrapbook.lottery.core.domain.Draw

/**
 * 遗漏保守派(设计 R3,missing 移植,mode=high):遗漏最久的 6 红 + 1 蓝(平局号码升序)。
 * 从未出现的号遗漏 = 期数 + 1,天然排最前。
 */
class MissingStrategy : Strategy {
    override val id = "missing"
    override val displayName = "遗漏保守派"
    override val requiresModel = false

    override fun pick(ctx: PickContext) =
        if (ctx.history.size < Analysis.MIN_HISTORY) {
            uniformPlan(ctx, id)
        } else {
            val m = Analysis.missing(ctx.history)
            val reds = mostMissed(m.red, Draw.RED_COUNT)
            singlePlan(id, reds, mostMissed(m.blue, 1).first())
        }

    /** 遗漏最高优先,平局号码升序。 */
    private fun mostMissed(counts: Map<Int, Int>, n: Int): List<Int> =
        counts.entries
            .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
            .take(n)
            .map { it.key }
            .sorted()
}
