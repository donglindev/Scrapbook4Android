package com.mb.scrapbook.lottery.core.domain

/** 奖级。一/二等奖金浮动,回放按假设值计(设计 R2)。 */
enum class PrizeTier(val label: String) {
    FIRST("一等奖"),
    SECOND("二等奖"),
    THIRD("三等奖"),
    FOURTH("四等奖"),
    FIFTH("五等奖"),
    SIXTH("六等奖"),
}

/** 一笔奖金。assumed=true 表示浮动奖按假设值计,账本须标注「假设值」。 */
data class Prize(val tier: PrizeTier, val amountYuan: Long, val assumed: Boolean = false)

/**
 * 对账表(设计 R2):命中 → 奖金。
 * 三等及以下为官方现行固定值(用户 2026-10-02 拍板);一/二等浮动,回放按 500 万/15 万假设值。
 */
object PrizeTable {
    const val FIRST_ASSUMED_YUAN = 5_000_000L
    const val SECOND_ASSUMED_YUAN = 1_500_000L

    fun prizeFor(hit: Hit): Prize? = when {
        hit.reds == 6 && hit.blue -> Prize(PrizeTier.FIRST, FIRST_ASSUMED_YUAN, assumed = true)
        hit.reds == 6 -> Prize(PrizeTier.SECOND, SECOND_ASSUMED_YUAN, assumed = true)
        hit.reds == 5 && hit.blue -> Prize(PrizeTier.THIRD, 3_000L)
        hit.reds == 5 || (hit.reds == 4 && hit.blue) -> Prize(PrizeTier.FOURTH, 200L)
        hit.reds == 4 || (hit.reds == 3 && hit.blue) -> Prize(PrizeTier.FIFTH, 10L)
        hit.blue -> Prize(PrizeTier.SIXTH, 5L)
        else -> null
    }
}
