package com.mb.scrapbook.lottery.arena

import com.mb.scrapbook.lottery.ArenaHonestyTestHelpers
import com.mb.scrapbook.lottery.core.arena.SeasonRunner
import com.mb.scrapbook.lottery.core.arena.SettleGateException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** T10-2 settle gate 拒绝/放行(D12=A):壳层仪器化复验(核心状态机在 JVM 层已有单测)。 */
class SettleGateInstrumentedTest {

    private val draw = ArenaHonestyTestHelpers.draw("26094")

    @Test
    fun gateRejectsAndListsMissing() {
        val runner = SeasonRunner(setOf("alpha", "beta"))
        runner.recordPick("alpha", "26094", ArenaHonestyTestHelpers.plan(), pickAt = 100L)
        val e = assertThrows(SettleGateException::class.java) { runner.settle(draw, enteredAt = 200L) }
        assertEquals(listOf("beta"), e.missing.sorted())
    }

    @Test
    fun gatePassesWhenAllPresentAndPersistsImmutability() {
        val runner = SeasonRunner(setOf("alpha", "beta"))
        runner.recordPick("alpha", "26094", ArenaHonestyTestHelpers.plan(), pickAt = 100L)
        runner.recordPick("beta", "26094", ArenaHonestyTestHelpers.plan(), pickAt = 110L)
        val results = runner.settle(draw, enteredAt = 200L)
        assertEquals(2, results.size)
        assertTrue(results.all { it.settlement.stakeYuan == 2L })
        assertThrows(IllegalStateException::class.java) { runner.settle(draw, enteredAt = 300L) } // D14 不可变
    }
}
