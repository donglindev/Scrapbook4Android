package com.mb.scrapbook.lottery.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DomainModelsTest {

    private fun draw(
        reds: List<Int> = listOf(1, 5, 11, 22, 28, 33),
        blue: Int = 9,
    ) = Draw(period = "26094", reds = reds, blue = blue, date = "2026-10-04", source = "test")

    // ---- Draw ----

    @Test
    fun validDrawPasses() {
        val d = draw()
        assertEquals(6, d.reds.size)
    }

    @Test
    fun drawRejectsWrongCount() {
        assertThrows(IllegalArgumentException::class.java) { draw(reds = listOf(1, 2, 3, 4, 5)) }
    }

    @Test
    fun drawRejectsDuplicate() {
        assertThrows(IllegalArgumentException::class.java) { draw(reds = listOf(1, 1, 2, 3, 4, 5)) }
    }

    @Test
    fun drawRejectsUnsorted() {
        assertThrows(IllegalArgumentException::class.java) { draw(reds = listOf(5, 1, 11, 22, 28, 33)) }
    }

    @Test
    fun drawRejectsOutOfRange() {
        assertThrows(IllegalArgumentException::class.java) { draw(reds = listOf(0, 5, 11, 22, 28, 33)) }
        assertThrows(IllegalArgumentException::class.java) { draw(reds = listOf(5, 11, 22, 28, 33, 34)) }
    }

    @Test
    fun drawRejectsBadBlue() {
        assertThrows(IllegalArgumentException::class.java) { draw(blue = 0) }
        assertThrows(IllegalArgumentException::class.java) { draw(blue = 17) }
    }

    // ---- BetScheme / Hit ----

    @Test
    fun betSchemeValidatesLikeDraw() {
        assertThrows(IllegalArgumentException::class.java) { BetScheme(listOf(1, 1, 2, 3, 4, 5), 9) }
        assertThrows(IllegalArgumentException::class.java) { BetScheme(listOf(1, 2, 3, 4, 5), 9) }
        assertThrows(IllegalArgumentException::class.java) { BetScheme(listOf(1, 2, 3, 4, 5, 6), 99) }
    }

    @Test
    fun hitCounts() {
        val scheme = BetScheme(listOf(1, 5, 11, 22, 30, 33), 9)
        val d = draw()
        assertEquals(Hit(5, true), scheme.hit(d))
    }

    @Test
    fun hitZeroPlusZero() {
        val scheme = BetScheme(listOf(2, 3, 4, 6, 7, 8), 1)
        assertEquals(Hit(0, false), scheme.hit(draw()))
    }

    // ---- NumberPool ----

    @Test
    fun numberPoolValidates() {
        NumberPool(setOf(1, 2, 3, 4, 5, 6), setOf(9), "freq")
        assertThrows(IllegalArgumentException::class.java) { NumberPool(setOf(1, 2, 3, 4, 5), setOf(9), "freq") }
        assertThrows(IllegalArgumentException::class.java) { NumberPool(setOf(1, 2, 3, 4, 5, 6), emptySet(), "freq") }
        assertThrows(IllegalArgumentException::class.java) { NumberPool(setOf(1, 2, 3, 4, 5, 34), setOf(9), "freq") }
        assertThrows(IllegalArgumentException::class.java) { NumberPool(setOf(1, 2, 3, 4, 5, 6), setOf(17), "freq") }
    }

    // ---- BetPlan ----

    @Test
    fun betPlanCostIsSchemesTimesPrice() {
        val schemes = (1..3).map { BetScheme(listOf(1, 2, 3, 4, 5, 6), 7) }
        val plan = BetPlan(schemes, generatedAt = 0L)
        assertEquals(6, plan.totalCost)
    }

    @Test
    fun betPlanRejectsEmpty() {
        assertThrows(IllegalArgumentException::class.java) { BetPlan(emptyList(), 0L) }
    }

    // ---- LedgerEntry ----

    @Test
    fun ledgerRoiMath() {
        val e = LedgerEntry("random", totalStakeYuan = 100, totalWinningsYuan = 150, hitDistribution = emptyMap())
        assertEquals(0.5, e.roi, 1e-9)
        assertEquals(0.0, LedgerEntry("random", 0, 0, emptyMap()).roi, 1e-9)
    }
}
