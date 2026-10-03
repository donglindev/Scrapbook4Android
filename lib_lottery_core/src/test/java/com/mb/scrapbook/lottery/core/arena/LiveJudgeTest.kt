package com.mb.scrapbook.lottery.core.arena

import com.mb.scrapbook.lottery.core.domain.BetPlan
import com.mb.scrapbook.lottery.core.domain.BetScheme
import com.mb.scrapbook.lottery.core.domain.Draw
import com.mb.scrapbook.lottery.core.domain.Verdict
import com.mb.scrapbook.lottery.core.replay.NullDistribution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveJudgeTest {

    /** 跑 n 期 live 赛季:alpha 每期出票,beta 每期出票但其中弃权 abstainAt 期。 */
    private fun runSeason(periods: Int, abstainAt: Set<Int> = emptySet()): Pair<List<Draw>, List<PeriodResult>> {
        val runner = SeasonRunner(setOf("alpha", "beta"))
        val draws = (0 until periods).map { i ->
            Draw("%05d".format(3001 + i), listOf(1, 5, 11, 22, 28, 33), 9, "2026-01-01", "test")
        }
        draws.forEachIndexed { i, d ->
            runner.recordPick("alpha", d.period, BetPlan(listOf(BetScheme(listOf(2, 4, 12, 21, 27, 32), 3)), 0L), pickAt = i * 1000L)
            if (i in abstainAt) {
                runner.recordAbstention("beta", d.period, "解析失败", at = i * 1000L + 10)
            } else {
                runner.recordPick("beta", d.period, BetPlan(listOf(BetScheme(listOf(3, 6, 13, 23, 29, 31), 5)), 0L), pickAt = i * 1000L + 10)
            }
            runner.settle(d, enteredAt = i * 1000L + 500)
        }
        return draws to runner.archive
    }

    @Test
    fun underThirtyPeriodsIsInsufficientSample() { // R5 切换点下侧
        val (draws, archive) = runSeason(LedgerAggregator.MIN_LIVE_PERIODS - 1)
        val judged = LedgerAggregator.judgeLive(archive, draws, NullDistribution(runs = 100, seed = 1L))
        judged.values.forEach {
            assertEquals(Verdict.INSUFFICIENT_SAMPLE, it.verdict)
            assertEquals(null, it.pValue)
        }
    }

    @Test
    fun atThirtyPeriodsJudgesWithHorizonMatchedNull() { // R5 切换点(含 30)
        val (draws, archive) = runSeason(LedgerAggregator.MIN_LIVE_PERIODS)
        val judged = LedgerAggregator.judgeLive(archive, draws, NullDistribution(runs = 100, seed = 1L))
        assertEquals(2, judged.size)
        judged.values.forEach {
            assertNotNull("30 期应有 p 值", it.pValue)
            assertTrue(it.verdict != Verdict.INSUFFICIENT_SAMPLE)
        }
    }

    @Test
    fun abstentionPeriodsContributeZeroStake() { // 弃权计 0 注(30 期中弃权 5 期)
        val (draws, archive) = runSeason(30, abstainAt = setOf(0, 1, 2, 3, 4))
        val ledger = LedgerAggregator.aggregate(archive)
        assertEquals(2L * 30, ledger.getValue("alpha").totalStakeYuan)
        assertEquals(2L * 25, ledger.getValue("beta").totalStakeYuan) // 5 期弃权 = 0 注
        // 有实际投入 → 仍可判决(horizon null 含弃权掩码)
        val judged = LedgerAggregator.judgeLive(archive, draws, NullDistribution(runs = 100, seed = 1L))
        assertNotNull(judged.getValue("beta").pValue)
    }

    @Test
    fun aggregateMatchesStakeAndWinnings() {
        val (_, archive) = runSeason(3)
        val ledger = LedgerAggregator.aggregate(archive)
        assertEquals(2L * 3, ledger.getValue("alpha").totalStakeYuan)
        assertEquals(3, ledger.getValue("alpha").hitDistribution.values.sum()) // 每期 1 注 → 3 条命中记录(无奖也计)
        assertTrue(ledger.getValue("alpha").hitDistribution.all { (hit, n) -> hit.reds in 0..6 })
    }
}
