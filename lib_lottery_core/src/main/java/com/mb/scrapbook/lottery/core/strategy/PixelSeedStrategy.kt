package com.mb.scrapbook.lottery.core.strategy

import com.mb.scrapbook.lottery.core.domain.BetPlan
import com.mb.scrapbook.lottery.core.domain.Draw
import java.security.MessageDigest
import kotlin.random.Random

/**
 * PixelSeed(设计 R6/R7):种子照片字节 SHA-256 → 赛季种子 → 每期 seed = hash(赛季种子 ‖ 期号) → 确定性出号。
 * 同一张照片永远同一组号(卖点级交互);不消费 ctx.rng。
 */
class PixelSeedStrategy : Strategy {
    override val id = "pixelseed"
    override val displayName = "PixelSeed"
    override val requiresModel = false

    override fun pick(ctx: PickContext): BetPlan {
        val seasonSeed = sha256(ctx.seedBytes)
        val periodSeed = sha256(seasonSeed + ctx.period.toByteArray())
        val rng = Random(periodSeed.take(Long.SIZE_BYTES).foldIndexed(0L) { i, acc, b ->
            acc or ((b.toLong() and 0xFF) shl (Byte.SIZE_BITS * i))
        })
        return singlePlan(id, Sampling.uniformReds(rng), Sampling.uniformBlue(rng))
    }

    private fun sha256(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)
}
