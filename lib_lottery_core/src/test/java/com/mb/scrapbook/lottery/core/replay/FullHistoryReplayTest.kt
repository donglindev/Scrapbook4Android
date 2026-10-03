package com.mb.scrapbook.lottery.core.replay

import com.mb.scrapbook.lottery.core.data.DrawRepository
import com.mb.scrapbook.lottery.core.strategy.ColdHotStrategy
import com.mb.scrapbook.lottery.core.strategy.FrequencyStrategy
import com.mb.scrapbook.lottery.core.strategy.MatrixStrategy
import com.mb.scrapbook.lottery.core.strategy.MissingStrategy
import com.mb.scrapbook.lottery.core.strategy.PixelSeedStrategy
import com.mb.scrapbook.lottery.core.strategy.RandomStrategy
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * 里程碑 A(R10 性能预算,非 CI):全量历史回放产出判决表。
 * 运行方式:FULL_REPLAY=1 ./gradlew :lib_lottery_core:test --tests "*FullHistory*"
 * 预算:6 位 JVM 选手 3490 期 + 共享零分布(1000 次)≤ 15 分钟。
 */
class FullHistoryReplayTest {

    @Test
    fun milestoneAVerdictTable() {
        assumeTrue("设 FULL_REPLAY=1 启用全量回放", System.getenv("FULL_REPLAY") == "1")
        val history = DrawRepository().load()
        val roster = listOf(
            RandomStrategy(), FrequencyStrategy(), ColdHotStrategy(),
            MissingStrategy(), MatrixStrategy(), PixelSeedStrategy(),
        )

        val t0 = System.currentTimeMillis()
        val result = WalkForwardReplay(roster, seed = WalkForwardReplay.DEFAULT_SEED).run(history)
        val tReplay = System.currentTimeMillis() - t0

        // 零分布:按(每期注数,期数)共享 —— 5 个 1 注/期策略共享同一份,matrix 46 注独立
        val t1 = System.currentTimeMillis()
        val nulls = NullDistribution()
        val shared1 = nulls.roi(history, 1)
        val matrixNull = nulls.roi(history, 46)
        val nullMap = roster.filter { it.id != "matrix" }.associate { it.id to shared1 } +
            mapOf("matrix" to matrixNull)
        val tNull = System.currentTimeMillis() - t1

        val judged = PValue.judge(result.ledgers.values.toList(), nullMap)
        val totalMin = (tReplay + tNull) / 60000.0

        println("MILESTONE-A | periods=${result.periods} | K=${judged.size} | threshold=${"%.4f".format(0.05 / judged.size)}")
        println("MILESTONE-A | replay=${tReplay}ms null=${tNull}ms total=${totalMin}min (budget 15min)")
        judged.sortedByDescending { it.roi }.forEach {
            println(
                "MILESTONE-A | ${it.strategyId.padEnd(10)} | stake=¥${it.totalStakeYuan} | win=¥${it.totalWinningsYuan} | " +
                    "roi=${"%.4f".format(it.roi)} | p=${it.pValue?.let { p -> PValue.format(p) }} | ${it.verdict}"
            )
        }
        assertTrue("总耗时 ${totalMin}min 超出 15 分钟预算", totalMin < 15.0)
    }
}
