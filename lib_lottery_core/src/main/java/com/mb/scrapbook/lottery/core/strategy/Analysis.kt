package com.mb.scrapbook.lottery.core.strategy

import com.mb.scrapbook.lottery.core.domain.Draw

/**
 * 历史统计分析(移植 lotterylab analyze 阶段的三个指标;max/avg 遗漏未移植——竞技场策略不消费)。
 * 全部确定性纯函数:同历史同结果。
 */
object Analysis {

    /** 冷启动阈值(设计 R5):统计策略历史不足时回退均匀采样,从第 1 期起确定可复现。 */
    const val MIN_HISTORY = 30

    /** 全历史出现次数。 */
    fun frequency(history: List<Draw>): BallCounts {
        val red = (1..Draw.RED_MAX).associateWithTo(LinkedHashMap()) { 0 }
        val blue = (1..Draw.BLUE_MAX).associateWithTo(LinkedHashMap()) { 0 }
        for (d in history) {
            for (num in d.reds) red[num] = red.getValue(num) + 1
            blue[d.blue] = blue.getValue(d.blue) + 1
        }
        return BallCounts(red, blue)
    }

    /** 最近 window 期出现次数(冷热号窗口,对齐 lotterylab AnalyzeStage 默认 30)。 */
    fun recentFrequency(history: List<Draw>, window: Int = 30): BallCounts =
        frequency(history.takeLast(minOf(window, history.size)))

    /**
     * 遗漏值:距最后一次出现的期数;从未出现 = 期数 + 1(对齐 lotterylab compute_missing_values)。
     */
    fun missing(history: List<Draw>): BallCounts {
        val lastIdx = history.size - 1
        val redLastSeen = HashMap<Int, Int>()
        val blueLastSeen = HashMap<Int, Int>()
        history.forEachIndexed { idx, d ->
            for (num in d.reds) redLastSeen[num] = idx
            blueLastSeen[d.blue] = idx
        }
        fun missingOf(lastSeen: Map<Int, Int>, upper: Int) =
            (1..upper).associateWithTo(LinkedHashMap()) { num ->
                val seen = lastSeen[num]
                if (seen != null) lastIdx - seen else lastIdx + 1
            }
        return BallCounts(missingOf(redLastSeen, Draw.RED_MAX), missingOf(blueLastSeen, Draw.BLUE_MAX))
    }
}

/** 红/蓝逐号计数(频率或遗漏值)。 */
data class BallCounts(val red: Map<Int, Int>, val blue: Map<Int, Int>)
