package com.mb.scrapbook.lottery.core.domain

/** 号码池:策略产出的候选集,供旋转矩阵缩水(设计 R2/R4)。红池≥6、蓝池≥1。 */
data class NumberPool(
    val redPool: Set<Int>,
    val bluePool: Set<Int>,
    val strategyName: String,
) {
    init {
        require(strategyName.isNotBlank()) { "strategyName 不能为空" }
        require(redPool.size >= Draw.RED_COUNT) { "红池至少 ${Draw.RED_COUNT} 个: $redPool" }
        require(redPool.all { it in 1..Draw.RED_MAX }) { "红池越界 1-${Draw.RED_MAX}: $redPool" }
        require(bluePool.isNotEmpty()) { "蓝池至少 1 个" }
        require(bluePool.all { it in 1..Draw.BLUE_MAX }) { "蓝池越界 1-${Draw.BLUE_MAX}: $bluePool" }
    }
}
