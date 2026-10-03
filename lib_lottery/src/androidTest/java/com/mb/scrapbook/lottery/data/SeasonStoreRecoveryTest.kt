package com.mb.scrapbook.lottery.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mb.scrapbook.lottery.ArenaHonestyTestHelpers
import com.mb.scrapbook.lottery.core.arena.SeasonRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** T10-1 赛季持久化 + 进程重启恢复(R7):结算产物落盘,新实例读回等价。 */
@RunWith(AndroidJUnit4::class)
class SeasonStoreRecoveryTest {

    private fun newStore() = SeasonStore(
        ApplicationProvider.getApplicationContext(),
        CoroutineScope(SupervisorJob() + Dispatchers.IO),
    )

    @Test
    fun settledResultsSurviveReload() = runBlocking {
        val store = newStore()
        // 首次进入:清场 + 打一期完整闭环
        store.dir.deleteRecursively()
        val fresh = newStore()
        fresh.load { ArenaHonestyTestHelpers.baseHistory() }
        fresh.startPeriodOverride = "26094"

        val runner = SeasonRunner(setOf("alpha"))
        runner.recordPick("alpha", "26094", ArenaHonestyTestHelpers.plan(), pickAt = 100L)
        val draw = ArenaHonestyTestHelpers.draw("26094")
        val results = runner.settle(draw, enteredAt = 200L)
        fresh.append(draw, results)

        // 「进程重启」:全新实例,从 filesDir 恢复
        val revived = newStore()
        revived.load { ArenaHonestyTestHelpers.baseHistory() }
        assertEquals(1, revived.archive.size)
        assertEquals("26094", revived.settledDraws.single().period)
        assertEquals(results.single(), revived.archive.single())
        assertTrue(revived.archive.single().timestamps.pickAt < revived.archive.single().timestamps.enteredAt)
        assertEquals("26095", revived.nextPeriod()) // 下一期正确推进
    }
}
