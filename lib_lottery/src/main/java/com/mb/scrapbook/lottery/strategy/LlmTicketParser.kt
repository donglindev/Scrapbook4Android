package com.mb.scrapbook.lottery.strategy

import com.google.gson.Gson
import com.mb.scrapbook.lottery.core.domain.Draw

/**
 * LLM 出票宽容解析(D24 第一阶梯:正则抓首个平衡 {} + 字段别名)。
 * 解析失败返回 null → 上层重试一次 → 仍失败弃权(弃权也是实验数据)。
 * 纯 JVM 可测;fixture 套件锁定成功率阈值 90%(D24)。
 */
object LlmTicketParser {

    data class ParsedTicket(val reds: List<Int>, val blue: Int, val reason: String)

    private val gson = Gson()

    fun parse(raw: String): ParsedTicket? {
        // 依次尝试每个平衡 {...} 候选(闲聊里的诱饵括号会被跳过)
        for (json in balancedJsonCandidates(raw)) {
            tryParse(json)?.let { return it }
        }
        return null
    }

    private fun tryParse(json: String): ParsedTicket? {
        val obj: Map<*, *> = try {
            gson.fromJson(json, Map::class.java)
        } catch (_: Exception) {
            return null
        } ?: return null

        val redsRaw = obj["reds"] ?: obj["red"] ?: obj["红球"] ?: return null
        val reds = (redsRaw as? List<*>)?.map { toIntOrNull(it) ?: return null } ?: return null
        val blue = toIntOrNull(obj["blue"] ?: obj["蓝球"] ?: return null) ?: return null
        val reason = obj["reason"]?.toString() ?: obj["理由"]?.toString() ?: ""

        // 语义校验(票面不变量)
        if (reds.size != Draw.RED_COUNT) return null
        if (reds.toSet().size != Draw.RED_COUNT) return null
        if (reds.any { it !in 1..Draw.RED_MAX }) return null
        if (blue !in 1..Draw.BLUE_MAX) return null
        return ParsedTicket(reds.sorted(), blue, reason.trim())
    }

    private fun toIntOrNull(v: Any?): Int? = when (v) {
        is Double -> if (v == Math.floor(v)) v.toInt() else null
        is Number -> v.toInt()
        is String -> v.trim().toIntOrNull()
        else -> null
    }

    /** 全部平衡 {...} 候选,按出现序(忽略字符串字面量内的花括号与截断残段)。 */
    fun balancedJsonCandidates(s: String): List<String> {
        val out = ArrayList<String>()
        var depth = 0
        var start = -1
        var inString = false
        var escaped = false
        for (i in s.indices) {
            val c = s[i]
            when {
                escaped -> escaped = false
                inString && c == '\\' -> escaped = true
                c == '"' -> inString = !inString
                !inString && c == '{' -> {
                    if (depth == 0) start = i
                    depth++
                }
                !inString && c == '}' && depth > 0 -> {
                    depth--
                    if (depth == 0 && start >= 0) {
                        out += s.substring(start, i + 1)
                        start = -1
                    }
                }
            }
        }
        return out
    }
}
