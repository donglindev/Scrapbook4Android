package com.mb.scrapbook.lottery.core.replay

import com.mb.scrapbook.lottery.core.domain.Draw
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class NullDistributionTest {

    private fun syntheticHistory(periods: Int, seed: Int = 11): List<Draw> {
        val rng = kotlin.random.Random(seed)
        return (0 until periods).map { i ->
            Draw("%05d".format(3001 + i), (1..33).shuffled(rng).take(6).sorted(), rng.nextInt(16) + 1, "2026", "t")
        }
    }

    @Test
    fun deterministicPerSeed() {
        val history = syntheticHistory(40)
        val a = NullDistribution(runs = 200, seed = 1L).roi(history, 1)
        val b = NullDistribution(runs = 200, seed = 1L).roi(history, 1)
        assertTrue(a.contentEquals(b))
    }

    @Test
    fun sameKeySharesInstance() { // C2:1 注/期策略共享同一份零分布
        val history = syntheticHistory(40)
        val nulls = NullDistribution(runs = 200, seed = 1L)
        assertSame(nulls.roi(history, 1), nulls.roi(history, 1))
        assertNotSame(nulls.roi(history, 1), nulls.roi(history, 46)) // 不同注数独立构造
    }

    @Test
    fun nullRoiIsNegativeOnAverage() { // 返奖率 ≈ 50% → 期望 ROI 显著为负
        val dist = NullDistribution(runs = 500, seed = 2L).roi(syntheticHistory(50), 1)
        val mean = dist.average()
        assertTrue("零分布均值 $mean 应在 (-0.9, -0.2)", mean in -0.9..-0.2)
    }

    @Test
    fun moreTicketsPerPeriodTightensRoi() { // 注量越大,stake-normalized ROI 方差越小
        val history = syntheticHistory(50)
        val nulls = NullDistribution(runs = 500, seed = 3L)
        val sd1 = sampleSd(nulls.roi(history, 1))
        val sd46 = sampleSd(nulls.roi(history, 46))
        assertTrue("sd46=$sd46 应明显小于 sd1=$sd1", sd46 < sd1 * 0.5)
    }

    private fun sampleSd(xs: DoubleArray): Double {
        val mean = xs.average()
        return kotlin.math.sqrt(xs.sumOf { (it - mean) * (it - mean) } / xs.size)
    }
}
