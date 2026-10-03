package com.mb.scrapbook.lottery.core.replay

import com.mb.scrapbook.lottery.core.domain.BetPlan
import com.mb.scrapbook.lottery.core.domain.Draw
import com.mb.scrapbook.lottery.core.strategy.ColdHotStrategy
import com.mb.scrapbook.lottery.core.strategy.FrequencyStrategy
import com.mb.scrapbook.lottery.core.strategy.MatrixStrategy
import com.mb.scrapbook.lottery.core.strategy.MissingStrategy
import com.mb.scrapbook.lottery.core.strategy.PickContext
import com.mb.scrapbook.lottery.core.strategy.PixelSeedStrategy
import com.mb.scrapbook.lottery.core.strategy.RandomStrategy
import com.mb.scrapbook.lottery.core.strategy.Strategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WalkForwardReplayTest {

    private val roster: List<Strategy> = listOf(
        RandomStrategy(), FrequencyStrategy(), ColdHotStrategy(),
        MissingStrategy(), MatrixStrategy(), PixelSeedStrategy(),
    )

    /** 伪随机但确定的历史:种子固定,画号互异合法。 */
    private fun syntheticHistory(periods: Int): List<Draw> {
        val rng = kotlin.random.Random(7)
        return (0 until periods).map { i ->
            val reds = (1..33).shuffled(rng).take(6).sorted()
            Draw("%05d".format(3001 + i), reds, rng.nextInt(16) + 1, "2026-01-01", "test")
        }
    }

    @Test
    fun sameSeedSameLedgers() { // R5 快照:同种子同账本
        val history = syntheticHistory(120)
        val a = WalkForwardReplay(roster, seed = 99L).run(history)
        val b = WalkForwardReplay(roster, seed = 99L).run(history)
        assertEquals(a.ledgers, b.ledgers)
        assertEquals(a.ticketsPerPeriod, b.ticketsPerPeriod)
    }

    @Test
    fun differentSeedDifferentRandomPicks() {
        val history = syntheticHistory(60)
        val a = WalkForwardReplay(listOf(RandomStrategy()), seed = 1L).run(history)
        val b = WalkForwardReplay(listOf(RandomStrategy()), seed = 2L).run(history)
        assertFalse(a.ledgers.getValue("random") == b.ledgers.getValue("random"))
    }

    @Test
    fun walkForwardSeesOnlyPast() { // 走前完整性:期 t 只见 [0,t)
        var calls = 0
        var violation: String? = null
        val spy = object : Strategy {
            override val id = "spy"
            override val displayName = "spy"
            override val requiresModel = false
            override fun pick(ctx: PickContext): BetPlan {
                if (ctx.history.size != calls) violation = "期 #$calls 却见到 ${ctx.history.size} 期历史"
                calls++
                return RandomStrategy().pick(ctx)
            }
        }
        WalkForwardReplay(listOf(spy), seed = 5L).run(syntheticHistory(50))
        assertEquals(50, calls)
        assertTrue("走前违约: $violation", violation == null)
    }

    @Test
    fun stakesMatchTicketCounts() {
        val history = syntheticHistory(80)
        val result = WalkForwardReplay(roster, seed = 3L).run(history)
        assertEquals(2L * 80, result.ledgers.getValue("random").totalStakeYuan)
        assertEquals(92L * 80, result.ledgers.getValue("matrix").totalStakeYuan) // 46 注 × ¥2
        assertEquals(1, result.ticketsPerPeriod.getValue("freq"))
        assertEquals(46, result.ticketsPerPeriod.getValue("matrix"))
        result.ledgers.values.forEach {
            assertTrue(it.totalWinningsYuan >= 0)
            assertTrue(it.hitDistribution.values.sum() > 0) // 有票必有命中记录(0+0 也计)
        }
    }
}
