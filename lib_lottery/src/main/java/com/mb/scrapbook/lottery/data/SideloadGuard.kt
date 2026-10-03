package com.mb.scrapbook.lottery.data

import com.mb.scrapbook.lottery.core.data.DrawRepository
import com.mb.scrapbook.lottery.core.domain.Draw
import java.io.StringReader

/**
 * 侧载 CSV 冲突守卫(D14=A):与已存期不一致时告警,只追加新期,旧账不动(已结算期不可变)。
 * 纯 JVM 可测;守卫结果由 UI 决定呈现(告警条/接受条数)。
 */
object SideloadGuard {

    data class Result(
        /** 可安全追加的新期(晚于现有最新期,且未与任何已存期冲突)。 */
        val newPeriods: List<Draw>,
        /** 冲突明细(同期不同号);含已结算期标记。 */
        val conflicts: List<String>,
    ) {
        val accepted: Boolean get() = conflicts.isEmpty()
    }

    /**
     * @param existing 现有完整历史(打包 + 已结算 live 期),仅用于冲突比对
     * @param settledPeriods 已结算期号集合(账本不可变,冲突只告警不改)
     */
    fun merge(existing: List<Draw>, settledPeriods: Set<String>, incoming: List<Draw>): Result {
        val existingByPeriod = existing.associateBy { it.period }
        val lastNum = existing.maxOfOrNull { it.period.toInt() } ?: 0
        val conflicts = mutableListOf<String>()
        val news = mutableListOf<Draw>()
        for (d in incoming) {
            val ex = existingByPeriod[d.period]
            when {
                ex == null && d.period.toInt() > lastNum -> news += d
                ex == null -> Unit // 空洞期(≤最新期的缺号):忽略,不回填
                ex.reds != d.reds || ex.blue != d.blue -> conflicts += buildString {
                    append("期 ${d.period}:已存 ${ex.reds}+${ex.blue} ≠ 侧载 ${d.reds}+${d.blue}")
                    if (d.period in settledPeriods) append("(已结算期,账本不可变)")
                }
            }
        }
        news.sortBy { it.period.toInt() }
        return Result(news, conflicts)
    }

    /** 侧载文件写回格式(与打包 CSV 同 schema)。 */
    fun toCsv(draws: List<Draw>): String = buildString {
        appendLine("period,red1,red2,red3,red4,red5,red6,blue,date,source")
        draws.forEach { d ->
            appendLine((listOf(d.period) + d.reds + listOf(d.blue, d.date, d.source.replace(',', '、'))).joinToString(","))
        }
    }

    /** 解析侧载内容(复用 core 严格解析,信任边界)。 */
    fun parseCsv(csv: String): List<Draw> = DrawRepository().parse(StringReader(csv))
}
