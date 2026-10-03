package com.mb.scrapbook.lottery.core.arena

import com.mb.scrapbook.lottery.core.domain.BetPlan
import com.mb.scrapbook.lottery.core.domain.BetScheme
import com.mb.scrapbook.lottery.core.domain.Draw
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Assert.fail
import org.junit.Test

class SeasonRunnerTest {

    private val participants = setOf("alpha", "beta")

    private fun draw(i: Int) = Draw(
        "%05d".format(3001 + i),
        listOf(1, 5, 11, 22, 28, 33),
        9,
        "2026-01-01",
        "test",
    )

    private fun plan() = BetPlan(listOf(BetScheme(listOf(1, 5, 11, 22, 28, 33), 9)), 0L)

    // ---- settle gate(D12=A) ----

    @Test
    fun gateRejectsIncompleteAndListsMissing() {
        val runner = SeasonRunner(participants)
        runner.recordPick("alpha", "03001", plan(), pickAt = 100L)
        try {
            runner.settle(draw(0), enteredAt = 200L)
            fail("应被 settle gate 拒绝")
        } catch (e: SettleGateException) {
            assertEquals(listOf("beta"), e.missing.sorted())
        }
    }

    @Test
    fun gatePassesWhenAllPresent() {
        val runner = SeasonRunner(participants)
        runner.recordPick("alpha", "03001", plan(), pickAt = 100L)
        runner.recordPick("beta", "03001", plan(), pickAt = 120L)
        val results = runner.settle(draw(0), enteredAt = 200L, settledAt = 201L)
        assertEquals(2, results.size)
        assertEquals(2, runner.archive.size)
        results.forEach {
            assertEquals(2L, it.settlement.stakeYuan)
            assertEquals(it.tickets.size, it.settlement.prizes.size) // 票↔奖 1:1
            assertTrue(it.timestamps.pickAt < it.timestamps.enteredAt)
        }
    }

    @Test
    fun pickAtNotBeforeEnteredFailsLoudly() { // D12:fail loudly
        val runner = SeasonRunner(participants)
        runner.recordPick("alpha", "03001", plan(), pickAt = 300L)
        runner.recordPick("beta", "03001", plan(), pickAt = 100L)
        val e = assertThrows(IllegalStateException::class.java) { runner.settle(draw(0), enteredAt = 200L) }
        assertTrue(e.message!!.contains("pickAt"))
    }

    // ---- 不可变(D14) ----

    @Test
    fun duplicateRecordRejected() {
        val runner = SeasonRunner(participants)
        runner.recordPick("alpha", "03001", plan(), pickAt = 100L)
        assertThrows(IllegalArgumentException::class.java) {
            runner.recordPick("alpha", "03001", plan(), pickAt = 110L)
        }
    }

    @Test
    fun settledPeriodIsImmutable() {
        val runner = SeasonRunner(participants)
        runner.recordPick("alpha", "03001", plan(), pickAt = 100L)
        runner.recordPick("beta", "03001", plan(), pickAt = 100L)
        runner.settle(draw(0), enteredAt = 200L)
        assertThrows(IllegalStateException::class.java) { runner.settle(draw(0), enteredAt = 300L) } // 重复结算
        assertThrows(IllegalStateException::class.java) { runner.recordPick("alpha", "03001", plan(), pickAt = 150L) } // 补票(状态违例)
    }

    // ---- 弃权 = 0 注 ----

    @Test
    fun abstentionCountsZeroStake() {
        val runner = SeasonRunner(participants)
        runner.recordPick("alpha", "03001", plan(), pickAt = 100L)
        runner.recordAbstention("beta", "03001", reason = "解析失败×2", at = 110L)
        val results = runner.settle(draw(0), enteredAt = 200L)
        val beta = results.first { it.strategyId == "beta" }
        assertTrue(beta.abstention)
        assertTrue(beta.tickets.isEmpty())
        assertEquals(0L, beta.settlement.stakeYuan)
        assertEquals(0L, beta.settlement.winningsYuan)
        assertEquals("解析失败×2", beta.reason)
    }

    @Test
    fun nonParticipantRejected() {
        val runner = SeasonRunner(participants)
        assertThrows(IllegalArgumentException::class.java) {
            runner.recordPick("ghost", "03001", plan(), pickAt = 100L)
        }
    }
}
