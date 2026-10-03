package com.mb.scrapbook.lottery.data

/** 开奖号手动录入校验(D14):即时红字 + 确认按钮门禁。纯 JVM 可测。 */
object DrawEntryValidator {

    /** 合法返回 null;非法返回用户可读错误文案。 */
    fun validate(reds: List<Int>, blue: Int): String? = when {
        reds.any { it == -1 } -> "红球未录满 6 个"
        reds.toSet().size != reds.size -> "红球重复: ${reds.filter { it > 0 }}"
        reds.any { it != -1 && it !in 1..33 } -> "红球越界 1-33"
        blue !in 1..16 -> "蓝球越界 1-16"
        else -> null
    }
}
