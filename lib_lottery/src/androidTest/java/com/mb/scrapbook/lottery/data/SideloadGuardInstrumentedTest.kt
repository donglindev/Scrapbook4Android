package com.mb.scrapbook.lottery.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mb.scrapbook.lottery.ArenaHonestyTestHelpers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** T10-4 侧载 CSV 冲突守卫(D14):真文件读写,冲突只告警、只追加、账本不动。 */
@RunWith(AndroidJUnit4::class)
class SideloadGuardInstrumentedTest {

    private fun newStore() = SeasonStore(
        ApplicationProvider.getApplicationContext(),
        CoroutineScope(SupervisorJob() + Dispatchers.IO),
    )

    @Test
    fun sideloadAppendsAndReportsConflicts() = runBlocking {
        newStore().dir.deleteRecursively()
        val store = newStore()
        store.load { ArenaHonestyTestHelpers.baseHistory() } // 26091-26093,蓝 9

        // ① 纯追加
        val ok = store.importSideload(
            """
                period,red1,red2,red3,red4,red5,red6,blue,date,source
                26091,1,5,11,22,28,33,9,2026-10-01,t
                26094,2,4,12,21,27,32,3,2026-10-03,t
            """.trimIndent()
        )
        assertTrue(ok.conflicts.isEmpty())
        assertEquals(listOf("26094"), ok.newPeriods.map { it.period })
        assertEquals("26094", store.baseHistory.last().period)

        // ② 冲突:改 26091 的蓝球 → 告警且旧数据不动
        val conflict = store.importSideload(
            """
                period,red1,red2,red3,red4,red5,red6,blue,date,source
                26091,1,5,11,22,28,33,16,2026-10-01,t
                26095,3,6,13,23,29,31,5,2026-10-05,t
            """.trimIndent()
        )
        assertEquals(1, conflict.conflicts.size)
        assertEquals(9, store.baseHistory.first().blue) // 旧数据未改
        assertEquals(listOf("26095"), conflict.newPeriods.map { it.period })

        // ③ 重启后侧载副本仍生效
        val revived = newStore()
        revived.load { ArenaHonestyTestHelpers.baseHistory() }
        assertEquals("26095", revived.baseHistory.last().period)
    }
}
