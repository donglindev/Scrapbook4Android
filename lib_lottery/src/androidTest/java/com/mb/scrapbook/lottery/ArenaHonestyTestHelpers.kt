package com.mb.scrapbook.lottery

import com.mb.scrapbook.lottery.core.domain.BetPlan
import com.mb.scrapbook.lottery.core.domain.BetScheme
import com.mb.scrapbook.lottery.core.domain.Draw

/** androidTest 共用夹具(D17=A 诚实机制套件)。 */
object ArenaHonestyTestHelpers {

    fun draw(period: String, reds: List<Int> = listOf(1, 5, 11, 22, 28, 33), blue: Int = 9) =
        Draw(period, reds, blue, "2026-10-01", "test")

    fun plan() = BetPlan(listOf(BetScheme(listOf(2, 4, 12, 21, 27, 32), 3)), 0L)

    fun baseHistory(): List<Draw> = listOf(draw("26091"), draw("26092"), draw("26093"))
}
