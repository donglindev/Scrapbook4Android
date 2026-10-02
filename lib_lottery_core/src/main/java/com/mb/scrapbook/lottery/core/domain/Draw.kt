package com.mb.scrapbook.lottery.core.domain

/**
 * 一期开奖(设计 R2,不变量对齐 lotterylab models)。
 * 红 6 个、1-33、不重复、升序;蓝 1-16。构造即校验,非法抛 IllegalArgumentException。
 */
data class Draw(
    val period: String,
    val reds: List<Int>,
    val blue: Int,
    val date: String,
    val source: String = "",
) {
    init {
        require(period.isNotBlank()) { "period 不能为空" }
        require(reds.size == RED_COUNT) { "红球必须 ${RED_COUNT} 个: $reds" }
        require(reds.toSet().size == RED_COUNT) { "红球重复: $reds" }
        require(reds.all { it in 1..RED_MAX }) { "红球越界 1-$RED_MAX: $reds" }
        require(reds == reds.sorted()) { "红球必须升序: $reds" }
        require(blue in 1..BLUE_MAX) { "蓝球越界 1-$BLUE_MAX: $blue" }
    }

    companion object {
        const val RED_COUNT = 6
        const val RED_MAX = 33
        const val BLUE_MAX = 16
    }
}
