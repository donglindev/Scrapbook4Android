package com.mb.scrapbook.lottery.core.strategy

import com.mb.scrapbook.lottery.core.domain.BetPlan

/** 纯随机对照组(设计 R3):均匀采样。科学底座,竞技场 UI 固定末位灰卡。 */
class RandomStrategy : Strategy {
    override val id = "random"
    override val displayName = "纯随机(对照组)"
    override val requiresModel = false

    override fun pick(ctx: PickContext): BetPlan = singlePlan(id, Sampling.uniformReds(ctx.rng), Sampling.uniformBlue(ctx.rng))
}
