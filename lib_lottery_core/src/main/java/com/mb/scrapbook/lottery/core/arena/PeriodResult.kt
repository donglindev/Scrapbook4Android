package com.mb.scrapbook.lottery.core.arena

import com.google.gson.Gson
import com.mb.scrapbook.lottery.core.domain.BetScheme

/**
 * 按期统一 JSON(设计 R8,工程评审 C1):filesDir 持久化与 v2 手机农场共用同一 schema,仅换传输通道。
 * D19:Gson 序列化。fromJson 是信任边界(农场/侧载来的 JSON)——Gson 反射绕过构造器校验,
 * validate() 在读入后重查全部不变量。
 */
data class PeriodResult(
    val strategyId: String = "",
    val period: String = "",
    /** 本期票据;弃权 = 空。 */
    val tickets: List<BetScheme> = emptyList(),
    /** 流式理由(LLM persona 输出或策略描述)。 */
    val reason: String = "",
    val settlement: Settlement = Settlement(),
    /** 显式弃权(语义纯净:仅指模型解析失败;计 0 注)。 */
    val abstention: Boolean = false,
    val timestamps: Timestamps = Timestamps(),
    /** 模型元信息(persona 名/温度/token 数);纯 JVM 选手为 null。 */
    val modelMeta: Map<String, String>? = null,
) {
    /** 对账:逐票命中与奖金。无奖票也占位(amount=0),保持票↔奖 1:1。 */
    data class Settlement(
        val prizes: List<TicketPrize> = emptyList(),
        val stakeYuan: Long = 0,
        val winningsYuan: Long = 0,
    )

    data class TicketPrize(
        val reds: Int = 0,
        val blueHit: Boolean = false,
        val tier: String = "",
        val amountYuan: Long = 0,
        val assumed: Boolean = false,
    )

    data class Timestamps(val pickAt: Long = 0, val enteredAt: Long = 0, val settledAt: Long = 0)

    /** 读入外部 JSON 后的不变量重查(D12/D19 信任边界)。 */
    fun validate(): PeriodResult {
        require(strategyId.isNotBlank()) { "strategyId 缺失" }
        require(period.isNotBlank()) { "period 缺失" }
        if (abstention) {
            require(tickets.isEmpty()) { "弃权期不应有票据: $tickets" }
        } else {
            require(tickets.isNotEmpty()) { "非弃权期至少 1 注" }
        }
        tickets.forEach { BetScheme(it.reds, it.blue) } // 重查票据不变量(构造器校验被 Gson 绕过)
        require(settlement.stakeYuan == tickets.size * 2L) { "stakeYuan ${settlement.stakeYuan} ≠ 注数×2" }
        require(settlement.prizes.size == tickets.size) { "prizes 应与票据 1:1" }
        require(timestamps.enteredAt == 0L || timestamps.pickAt < timestamps.enteredAt) {
            "数据完整性违例: pickAt(${timestamps.pickAt}) >= enteredAt(${timestamps.enteredAt})"
        }
        return this
    }

    companion object {
        private val gson = Gson()

        fun toJson(r: PeriodResult): String = gson.toJson(r)

        /** 信任边界入口:缺字段(NPE)与违约都归一为 IllegalArgumentException。 */
        fun fromJson(json: String): PeriodResult = try {
            gson.fromJson(json, PeriodResult::class.java).validate()
        } catch (e: NullPointerException) {
            throw IllegalArgumentException("PeriodResult JSON 缺字段", e)
        }
    }
}
