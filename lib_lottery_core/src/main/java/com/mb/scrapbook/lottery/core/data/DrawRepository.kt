package com.mb.scrapbook.lottery.core.data

import com.mb.scrapbook.lottery.core.domain.Draw
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.Reader

/**
 * ssq_history.csv 读取(设计 R6;D11=A: 唯一副本在 core main resources,classpath 单源)。
 * 打包历史走 load();侧载/测试通道复用 parse()。
 */
class DrawRepository(
    private val classLoader: ClassLoader = DrawRepository::class.java.classLoader,
) {

    /** 打包历史。资源缺失抛 IllegalStateException(报错不崩,设计 R10)。 */
    fun load(): List<Draw> {
        val input = classLoader.getResourceAsStream(RESOURCE)
            ?: throw IllegalStateException(
                "打包历史缺失: $RESOURCE —— 唯一副本应在 lib_lottery_core/src/main/resources/ (D11=A)"
            )
        return input.use { parse(InputStreamReader(it, Charsets.UTF_8)) }
    }

    /**
     * CSV 解析(schema: period,red1..red6,blue,date,source)。
     * 缺列/越界号/乱序期号 → IllegalArgumentException(带行号)——侧载是信任边界,不静默容忍。
     * 红球列序视为呈现格式,解析时统一排序(Draw 不变量要求升序)。
     */
    fun parse(reader: Reader): List<Draw> {
        val lines = BufferedReader(reader).readLines()
        val headerIndex = lines.indexOfFirst { it.isNotBlank() }
        require(headerIndex >= 0) { "CSV 为空" }
        val header = lines[headerIndex].trim().removePrefix(BOM) // 剥 UTF-8 BOM(侧载文件可能带)
        require(header == HEADER) { "表头不符: $header" }

        val draws = ArrayList<Draw>(lines.size)
        var prevPeriod = -1
        for (i in headerIndex + 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            val cols = line.split(',')
            require(cols.size == COLUMN_COUNT) { "第${i + 1}行列数 ${cols.size} ≠ $COLUMN_COUNT: $line" }
            val periodNum = cols[0].trim().toIntOrNull()
                ?: throw IllegalArgumentException("第${i + 1}行期号非数字: ${cols[0]}")
            require(periodNum > prevPeriod) { "第${i + 1}行期号乱序或重复: ${cols[0]}" }
            val reds = (1..6).map { j ->
                cols[j].trim().toIntOrNull()
                    ?: throw IllegalArgumentException("第${i + 1}行红球非数字: ${cols[j]}")
            }.sorted()
            val blue = cols[7].trim().toIntOrNull()
                ?: throw IllegalArgumentException("第${i + 1}行蓝球非数字: ${cols[7]}")
            draws += Draw(cols[0].trim(), reds, blue, cols[8].trim(), cols[9].trim())
            prevPeriod = periodNum
        }
        require(draws.isNotEmpty()) { "CSV 无数据行" }
        return draws
    }

    companion object {
        private val BOM = 0xFEFF.toChar().toString() // UTF-8 BOM
        const val RESOURCE = "ssq_history.csv"
        const val COLUMN_COUNT = 10
        private const val HEADER = "period,red1,red2,red3,red4,red5,red6,blue,date,source"
    }
}
