package com.mb.scrapbook.lottery.strategy

import com.google.gson.Gson
import com.mb.scrapbook.lottery.core.domain.BetPlan
import com.mb.scrapbook.lottery.core.domain.BetScheme
import com.mb.scrapbook.lottery.core.domain.Draw
import com.mb.scrapbook.lottery.core.strategy.Analysis
import com.mb.scrapbook.lottery.infer.LlamaEngine
import kotlinx.coroutines.flow.collect

/**
 * LLM persona 出票(R7):统计 JSON → persona 提示词 → 流式输出(onToken 供聚光灯演出)
 * → 宽容解析 → 失败重试 1 次 → 弃权(显式记录,计 0 注)。
 * D24:fixture 成功率 ≥90% 则停留本实现;GBNF 升级仅在实测 <90% 且成本 <半天时启动。
 */
class LlmPersonaPicker(private val engine: LlamaEngine) {

    sealed class Outcome {
        data class Ticket(val plan: BetPlan, val reason: String) : Outcome()
        data class Abstain(val reason: String) : Outcome()
    }

    private val gson = Gson()

    suspend fun pick(
        persona: Persona,
        period: String,
        history: List<Draw>,
        onToken: (String) -> Unit = {},
    ): Outcome {
        engine.setTemperature(persona.temperature)
        val stats = statsJson(history)

        val first = generate(buildPrompt(persona, period, stats, retry = false), onToken)
        LlmTicketParser.parse(first)?.let { return it.toOutcome(persona) }

        val second = generate(buildPrompt(persona, period, stats, retry = true), onToken)
        LlmTicketParser.parse(second)?.let { return it.toOutcome(persona) }

        return Outcome.Abstain("两次解析失败:(${first.take(60)}…)")
    }

    private suspend fun generate(prompt: String, onToken: (String) -> Unit): String {
        val sb = StringBuilder()
        engine.sendUserPrompt(prompt, predictLength = 512).collect { token ->
            sb.append(token)
            onToken(token)
        }
        return sb.toString()
    }

    private fun LlmTicketParser.ParsedTicket.toOutcome(persona: Persona) = Outcome.Ticket(
        plan = BetPlan(
            schemes = listOf(BetScheme(reds, blue)),
            generatedAt = 0L,
            config = mapOf("strategy" to persona.id, "temperature" to persona.temperature.toString()),
        ),
        reason = reason,
    )

    /** 出票提示词:persona 人设 + 紧凑统计 JSON + 严格格式要求。 */
    fun buildPrompt(persona: Persona, period: String, stats: String, retry: Boolean): String = buildString {
        append(persona.personaSection())
        append("第 $period 期。近 30 期统计(红球按频次排序,遗漏为期数):")
        append(stats)
        append('\n')
        if (retry) {
            append("你上一次的输出无法解析为合法票据。这一次:只输出一个 JSON 对象," +
                "格式 {\"reds\":[6个互异的1-33整数],\"blue\":1-16整数,\"reason\":\"...\"},不要有任何多余文字。")
        } else {
            append("按你的角色风格选出一注(6 红 1 蓝),只输出一个 JSON 对象:" +
                "{\"reds\":[...],\"blue\":...,\"reason\":\"不超过40字\"}。")
        }
    }

    /** 紧凑统计:近 30 期红/蓝频次 top 与遗漏 top(给 LLM 的风格素材,不是先验)。 */
    private fun statsJson(history: List<Draw>): String {
        if (history.size < Analysis.MIN_HISTORY) return "{\"note\":\"历史不足30期,自由发挥\"}"
        val recent = Analysis.recentFrequency(history.takeLast(30))
        val missing = Analysis.missing(history.takeLast(30))
        fun top(m: Map<Int, Int>, n: Int, desc: Boolean) =
            m.entries.sortedBy { if (desc) -it.value else it.value }.take(n).map { it.key }
        val payload = linkedMapOf(
            "red_hot_30" to top(recent.red, 10, desc = true),
            "red_cold_30" to top(recent.red, 10, desc = false),
            "red_missing_top" to top(missing.red, 10, desc = true),
            "blue_hot_30" to top(recent.blue, 5, desc = true),
            "blue_missing_top" to top(missing.blue, 5, desc = true),
        )
        return gson.toJson(payload)
    }
}
