package com.mb.scrapbook.lottery.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 奖级命中边界全覆盖(设计 R10 评审增补:PrizeTable 命中边界)。 */
class PrizeTableTest {

    private fun prize(reds: Int, blue: Boolean) = PrizeTable.prizeFor(Hit(reds, blue))

    @Test
    fun firstPrize() {
        val p = prize(6, blue = true)!!
        assertEquals(PrizeTier.FIRST, p.tier)
        assertEquals(5_000_000L, p.amountYuan)
        assertTrue(p.assumed)
    }

    @Test
    fun secondPrize() {
        val p = prize(6, blue = false)!!
        assertEquals(PrizeTier.SECOND, p.tier)
        assertEquals(1_500_000L, p.amountYuan)
        assertTrue(p.assumed)
    }

    @Test
    fun thirdPrize() {
        val p = prize(5, blue = true)!!
        assertEquals(PrizeTier.THIRD, p.tier)
        assertEquals(3_000L, p.amountYuan)
        assertFalse(p.assumed)
    }

    @Test
    fun fourthPrize() {
        assertEquals(200L, prize(5, blue = false)!!.amountYuan)
        assertEquals(200L, prize(4, blue = true)!!.amountYuan)
    }

    @Test
    fun fifthPrize() {
        assertEquals(10L, prize(4, blue = false)!!.amountYuan)
        assertEquals(10L, prize(3, blue = true)!!.amountYuan)
    }

    @Test
    fun sixthPrize() {
        assertEquals(5L, prize(2, blue = true)!!.amountYuan)
        assertEquals(5L, prize(1, blue = true)!!.amountYuan)
        assertEquals(5L, prize(0, blue = true)!!.amountYuan)
    }

    @Test
    fun noPrizeBelowThreshold() {
        assertNull(prize(0, blue = false))
        assertNull(prize(1, blue = false))
        assertNull(prize(2, blue = false))
        assertNull(prize(3, blue = false)) // 3红0蓝不中奖
    }

    @Test
    fun fixedTiersNotAssumed() {
        listOf(5 to false, 4 to true, 4 to false, 3 to true, 2 to true).forEach { (r, b) ->
            assertFalse(prize(r, b)!!.assumed)
        }
    }
}
