package com.mb.scrapbook.lottery.core.domain

/** 一期出票计划(设计 R2):若干注 + 生成时间 + 策略配置快照。totalCost = 注数 × ¥2。 */
data class BetPlan(
    val schemes: List<BetScheme>,
    val generatedAt: Long,
    val config: Map<String, String> = emptyMap(),
) {
    init {
        require(schemes.isNotEmpty()) { "出票计划至少 1 注" }
    }

    val totalCost: Int get() = schemes.size * TICKET_PRICE_YUAN

    companion object {
        const val TICKET_PRICE_YUAN = 2
    }
}
