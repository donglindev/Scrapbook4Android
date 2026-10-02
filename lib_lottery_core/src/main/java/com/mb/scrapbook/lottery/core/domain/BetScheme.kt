package com.mb.scrapbook.lottery.core.domain

/** 一注投注:6 红 + 1 蓝,构造即校验。 */
data class BetScheme(
    val reds: List<Int>,
    val blue: Int,
) {
    init {
        require(reds.size == Draw.RED_COUNT) { "红球必须 ${Draw.RED_COUNT} 个: $reds" }
        require(reds.toSet().size == Draw.RED_COUNT) { "红球重复: $reds" }
        require(reds.all { it in 1..Draw.RED_MAX }) { "红球越界 1-${Draw.RED_MAX}: $reds" }
        require(blue in 1..Draw.BLUE_MAX) { "蓝球越界 1-${Draw.BLUE_MAX}: $blue" }
    }

    /** 对账:红中几个、蓝是否命中。 */
    fun hit(draw: Draw): Hit = Hit(reds.count { it in draw.reds }, blue == draw.blue)
}

/** 命中结果,账本命中分布的键(6+1 … 0+0)。 */
data class Hit(val reds: Int, val blue: Boolean)
