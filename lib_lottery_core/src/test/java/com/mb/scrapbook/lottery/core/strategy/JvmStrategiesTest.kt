package com.mb.scrapbook.lottery.core.strategy

import com.mb.scrapbook.lottery.core.domain.Draw
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class JvmStrategiesTest {

    private fun ctx(history: List<Draw>, seed: Int = 42, period: String = "26094") =
        PickContext(period, history, Random(seed), byteArrayOf(1, 2, 3))

    private fun syntheticHistory(
        count: Int = 40,
        reds: List<Int> = listOf(1, 2, 3, 4, 5, 6),
        firstReds: List<Int>? = null, // 仅第 1 期替换
        blue: Int = 7,
        firstBlue: Int? = null,
    ): List<Draw> = (0 until count).map { i ->
        Draw(
            period = "%05d".format(3001 + i),
            reds = (if (i == 0 && firstReds != null) firstReds else reds).sorted(),
            blue = if (i == 0 && firstBlue != null) firstBlue else blue,
            date = "2026-01-01",
            source = "test",
        )
    }

    // ---- 通用 ----

    @Test
    fun sixJvmStrategiesExistWithUniqueIds() {
        val all = listOf(
            RandomStrategy(), FrequencyStrategy(), ColdHotStrategy(),
            MissingStrategy(), MatrixStrategy(), PixelSeedStrategy(),
        )
        assertEquals(6, all.size)
        assertTrue(all.none { it.requiresModel })
        assertEquals(6, all.map { it.id }.toSet().size)
    }

    @Test
    fun randomIsDeterministicPerRngSeed() {
        val history = syntheticHistory()
        assertEquals(RandomStrategy().pick(ctx(history, seed = 7)), RandomStrategy().pick(ctx(history, seed = 7)))
        assertFalse(RandomStrategy().pick(ctx(history, seed = 7)).schemes.first().reds ==
            RandomStrategy().pick(ctx(history, seed = 8)).schemes.first().reds)
    }

    @Test
    fun statisticalStrategiesColdStartFallBackToUniform() {
        val shortHistory = syntheticHistory(count = Analysis.MIN_HISTORY - 1)
        for (s in listOf<Strategy>(FrequencyStrategy(), ColdHotStrategy(), MissingStrategy())) {
            val plan = s.pick(ctx(shortHistory))
            assertEquals(1, plan.schemes.size)
            assertEquals(2, plan.totalCost)
            assertEquals(s.id, plan.config["strategy"]) // config 记录真实策略 id
        }
    }

    // ---- 频率派:权重生效 ----

    @Test
    fun freqWeightedPickPrefersHotNumbers() {
        // 40 期红球恒为 1-6、蓝恒为 7 → 零权重号不可达,采样必然命中热号
        val history = syntheticHistory()
        val plan = FrequencyStrategy().pick(ctx(history))
        assertEquals(listOf(1, 2, 3, 4, 5, 6), plan.schemes.first().reds)
        assertEquals(7, plan.schemes.first().blue)
    }

    @Test
    fun freqPoolIsDeterministicTop12() {
        val history = syntheticHistory()
        val pool1 = FrequencyStrategy().pool(ctx(history))
        val pool2 = FrequencyStrategy().pool(ctx(history))
        assertEquals(pool1, pool2)
        assertEquals(12, pool1.redPool.size)
        assertEquals(1, pool1.bluePool.size)
        assertTrue(1..6 allIn pool1.redPool) // 热号全在池内
        assertTrue(pool1.redPool.none { it > 12 }) // 零权重号平局升序取前 12:即 1-6(热) + 7-12
    }

    private infix fun IntRange.allIn(set: Set<Int>) = this.all { it in set }

    // ---- 冷号猎手 ----

    @Test
    fun coldHotPicksColdestRecentNumbers() {
        // 近 30 期只出过红 1-6、蓝 7 → 最冷红 = 7-12(零频平局号码升序),最冷蓝 = 1
        val plan = ColdHotStrategy().pick(ctx(syntheticHistory()))
        assertEquals(listOf(7, 8, 9, 10, 11, 12), plan.schemes.first().reds)
        assertEquals(1, plan.schemes.first().blue)
    }

    // ---- 遗漏保守派 ----

    @Test
    fun missingPicksLongestUnseenNumbers() {
        // 第 1 期出红 33/蓝 16,之后 39 期恒为红 1-6/蓝 7:
        // 从未出现(红 7-32、蓝 1-6/8-16)遗漏 = 期数+1 = 40,高于 33/16 的 39 → 取平局升序前 6/前 1
        val history = syntheticHistory(
            firstReds = listOf(1, 2, 3, 4, 5, 33),
            firstBlue = 16,
        )
        val plan = MissingStrategy().pick(ctx(history))
        assertEquals(listOf(7, 8, 9, 10, 11, 12), plan.schemes.first().reds)
        assertEquals(1, plan.schemes.first().blue)
    }

    // ---- PixelSeed 种子链 ----

    @Test
    fun samePhotoSamePeriodSameTicket() {
        val history = syntheticHistory()
        assertEquals(
            PixelSeedStrategy().pick(ctx(history, seed = 1)),
            PixelSeedStrategy().pick(ctx(history, seed = 99)), // 不消费 ctx.rng,种子不同结果仍同
        )
    }

    @Test
    fun differentPeriodDifferentTicket() {
        val history = syntheticHistory()
        val a = PixelSeedStrategy().pick(ctx(history, period = "26094"))
        val b = PixelSeedStrategy().pick(ctx(history, period = "26095"))
        assertFalse(a.schemes.first().reds == b.schemes.first().reds && a.schemes.first().blue == b.schemes.first().blue)
    }
}
