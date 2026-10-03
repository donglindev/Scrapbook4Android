package com.mb.scrapbook.lottery.core.strategy

import com.mb.scrapbook.lottery.core.domain.Draw
import com.mb.scrapbook.lottery.core.domain.NumberPool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatrixStrategyTest {

    private val pool12 = NumberPool((1..12).toSet(), setOf(7), "freq")

    // ---- 核心不变量:全前提覆盖(R4 硬断言) ----

    @Test
    fun shrinkCoversAllPremises() {
        for ((p, t) in listOf(MatrixStrategy.DEFAULT_P to MatrixStrategy.DEFAULT_T, 7 to 4, 6 to 3, 8 to 5)) {
            val plan = MatrixStrategy(p, t).shrink(pool12)
            assertFullCoverage((1..12).toList(), p, t, plan.schemes.map { it.reds })
        }
    }

    @Test
    fun defaultPresetMatchesD20Calibration() { // D20-B 拍板:中6保5 → 恰 46 注 = ¥92(贪心确定性,精确断言)
        val plan = MatrixStrategy().shrink(pool12)
        assertEquals(46, plan.schemes.size)
        assertEquals(92, plan.totalCost)
    }

    @Test
    fun blueFullyPaired() {
        val twoBlues = NumberPool((1..12).toSet(), setOf(3, 9), "freq")
        val plan = MatrixStrategy().shrink(twoBlues)
        val redCombos = plan.schemes.map { it.reds }.distinct()
        assertEquals(redCombos.size * 2, plan.schemes.size) // 每红组合 × 每蓝
        assertTrue(plan.schemes.all { it.blue == 3 || it.blue == 9 })
    }

    @Test
    fun n6Shortcut() {
        val plan = MatrixStrategy(p = 7, t = 4).shrink(NumberPool((5..10).toSet(), setOf(7, 8), "freq"))
        assertEquals(2, plan.schemes.size) // 唯一 6 红 × 2 蓝
        assertTrue(plan.schemes.all { it.reds == (5..10).toList() })
    }

    @Test
    fun shrinkIsDeterministic() {
        assertEquals(MatrixStrategy(7, 4).shrink(pool12), MatrixStrategy(7, 4).shrink(pool12))
    }

    @Test
    fun pickEndToEndViaFrequencyPool() {
        val history = (0 until 40).map { i ->
            Draw("%05d".format(3001 + i), listOf(1, 2, 3, 4, 5, 6), 7, "2026-01-01", "test")
        }
        val ctx = PickContext("26094", history, kotlin.random.Random(42))
        val plan = MatrixStrategy().pick(ctx)
        val freqPool = FrequencyStrategy().pool(ctx).redPool
        assertTrue(plan.schemes.all { it.reds.all { r -> r in freqPool } }) // 票 ⊆ 频率池
        assertFullCoverage(freqPool.sorted(), MatrixStrategy.DEFAULT_P, MatrixStrategy.DEFAULT_T, plan.schemes.map { it.reds })
        // 冷启动:历史 < 30 也确定可复现(每次出票用全新同种子 ctx——pick 会消费 rng)
        fun freshShortCtx() = PickContext("26094", history.take(29), kotlin.random.Random(42))
        assertEquals(MatrixStrategy().pick(freshShortCtx()), MatrixStrategy().pick(freshShortCtx()))
    }

    // ---- D20 校准网格(已拍板 B=P6/T5):保留全覆盖断言 + 注数/耗时报告(输出读自测试 XML system-out) ----

    @Test
    fun calibrationGrid() {
        val grid = listOf(7 to 4, 6 to 4, 8 to 4, 7 to 3, 7 to 5, 6 to 3, 8 to 3, 6 to 5, 8 to 5)
        for ((p, t) in grid) {
            val start = System.nanoTime()
            val plan = MatrixStrategy(p, t).shrink(pool12)
            val ms = (System.nanoTime() - start) / 1e6
            assertFullCoverage((1..12).toList(), p, t, plan.schemes.map { it.reds })
            println("D20-CALIBRATION | P=$p T=$t | ${plan.schemes.size} 注 | ¥${plan.totalCost} | ${ms.toInt()}ms")
        }
    }

    /** R4 验收断言:对每个 P 子集前提,至少一注与其命中 ≥ T。 */
    private fun assertFullCoverage(numbers: List<Int>, p: Int, t: Int, tickets: List<List<Int>>) {
        for (combo in combinations(numbers.size, p)) {
            val premise = combo.map { numbers[it] }.toSet()
            assertTrue(
                "p=$p t=$t 前提 $premise 未被任何票覆盖",
                tickets.any { ticket -> ticket.count { it in premise } >= t },
            )
        }
    }
}
