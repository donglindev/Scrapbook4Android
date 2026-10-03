package com.mb.scrapbook.lottery.core.arena

import com.mb.scrapbook.lottery.core.domain.BetPlan
import com.mb.scrapbook.lottery.core.domain.BetScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PeriodResultTest {

    private fun sample() = PeriodResult(
        strategyId = "llm_professor",
        period = "26094",
        tickets = listOf(BetScheme(listOf(1, 5, 11, 22, 28, 33), 9)),
        reason = "红球频率分布显示 1 区间过热…",
        settlement = PeriodResult.Settlement(
            prizes = listOf(PeriodResult.TicketPrize(reds = 6, blueHit = true, tier = "FIRST", amountYuan = 5_000_000, assumed = true)),
            stakeYuan = 2,
            winningsYuan = 5_000_000,
        ),
        timestamps = PeriodResult.Timestamps(pickAt = 100, enteredAt = 200, settledAt = 201),
        modelMeta = mapOf("persona" to "统计教授", "temperature" to "0.3"),
    )

    @Test
    fun jsonRoundTripPreservesEverything() { // D19:Gson 统一 schema
        val r = sample()
        assertEquals(r, PeriodResult.fromJson(PeriodResult.toJson(r)))
    }

    @Test
    fun fromJsonRejectsInvalidTicket() { // 信任边界:越界红球
        val json = """
            {"strategyId":"x","period":"26094",
             "tickets":[{"reds":[1,2,3,4,5,34],"blue":7}],
             "settlement":{"prizes":[],"stakeYuan":2,"winningsYuan":0},
             "timestamps":{"pickAt":1,"enteredAt":2,"settledAt":3}}
        """.trimIndent()
        assertThrows(IllegalArgumentException::class.java) { PeriodResult.fromJson(json) }
    }

    @Test
    fun fromJsonRejectsPickAtNotBeforeEnteredAt() { // D12 断言延伸到 JSON 层
        val json = """
            {"strategyId":"x","period":"26094",
             "tickets":[{"reds":[1,2,3,4,5,6],"blue":7}],
             "settlement":{"prizes":[],"stakeYuan":2,"winningsYuan":0},
             "timestamps":{"pickAt":5,"enteredAt":5,"settledAt":6}}
        """.trimIndent()
        assertThrows(IllegalArgumentException::class.java) { PeriodResult.fromJson(json) }
    }

    @Test
    fun fromJsonRejectsMissingIdentity() {
        assertThrows(IllegalArgumentException::class.java) { PeriodResult.fromJson("{\"period\":\"26094\"}") }
    }
}
