package com.mb.scrapbook.lottery.data

import com.mb.scrapbook.lottery.core.domain.Draw
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SideloadGuardTest {

    private fun draw(period: String, reds: List<Int> = listOf(1, 5, 11, 22, 28, 33), blue: Int = 9) =
        Draw(period, reds, blue, "2026-10-01", "test")

    private val base = listOf(draw("26091"), draw("26092"), draw("26093"))
    private val settled = setOf("26094")

    @Test
    fun strictExtensionAccepted() {
        val incoming = base + draw("26094") + draw("26095", blue = 5)
        val r = SideloadGuard.merge(base + draw("26094"), settled, incoming)
        assertTrue(r.accepted)
        assertEquals(listOf("26095"), r.newPeriods.map { it.period })
    }

    @Test
    fun conflictWithSettledWarnsAndKeepsLedgerImmutable() {
        // 已结算 26094 = 红不同;侧载 26094' 与之冲突 → 只告警,不改,新期照常追加
        val incoming = base + draw("26094", reds = listOf(2, 6, 12, 23, 29, 31)) + draw("26095")
        val r = SideloadGuard.merge(base + draw("26094"), settled, incoming)
        assertEquals(1, r.conflicts.size)
        assertTrue(r.conflicts.first().contains("已结算期,账本不可变"))
        assertEquals(listOf("26095"), r.newPeriods.map { it.period })
    }

    @Test
    fun conflictWithBaseWarns() {
        val incoming = base.mapIndexed { i, d -> if (i == 1) draw("26092", blue = 16) else d } + draw("26094")
        val r = SideloadGuard.merge(base, emptySet(), incoming)
        assertEquals(1, r.conflicts.size)
        assertTrue(r.conflicts.first().contains("26092"))
        assertEquals(listOf("26094"), r.newPeriods.map { it.period })
    }

    @Test
    fun sameNumbersAreNoConflict() {
        val r = SideloadGuard.merge(base, emptySet(), base)
        assertTrue(r.accepted)
        assertTrue(r.newPeriods.isEmpty())
    }

    @Test
    fun olderOnlyCsvAddsNothing() {
        val r = SideloadGuard.merge(base, emptySet(), base.take(2))
        assertTrue(r.accepted)
        assertTrue(r.newPeriods.isEmpty())
    }

    @Test
    fun csvRoundTripPreservesDraws() {
        val csv = SideloadGuard.toCsv(base + draw("26094", reds = listOf(3, 7, 13, 19, 25, 31), blue = 4))
        val parsed = SideloadGuard.parseCsv(csv)
        assertEquals(base + draw("26094", reds = listOf(3, 7, 13, 19, 25, 31), blue = 4), parsed)
    }

    @Test(expected = IllegalArgumentException::class)
    fun malformedCsvRejected() {
        SideloadGuard.parseCsv("period,red1\n26091,1")
    }
}
