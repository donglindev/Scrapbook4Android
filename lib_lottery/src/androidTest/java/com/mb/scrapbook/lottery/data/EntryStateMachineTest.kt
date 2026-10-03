package com.mb.scrapbook.lottery.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** T10-3 录入确认/作废重录状态机(D14):即时校验拒绝非法,确认入账,重录清空。 */
@RunWith(AndroidJUnit4::class)
class EntryStateMachineTest {

    private val validator = DrawEntryValidator

    @Test
    fun invalidEntriesRejectedWithMessages() {
        assertNotNull(validator.validate(listOf(1, 1, 11, 22, 28, 33), 9)) // 重复
        assertNotNull(validator.validate(listOf(1, 5, 11, 22, 28, 34), 9)) // 越界
        assertNotNull(validator.validate(listOf(1, 5, 11, 22, 28), 9))     // 不足
        assertNull(validator.validate(listOf(1, 5, 11, 22, 28, 33), 9))    // 合法
    }

    /** 端到端:VM 状态机 出票→非法录入被拒→合法录入结算。 */
    @Test
    fun drawNightEndToEnd() = runBlocking {
        ApplicationProvider.getApplicationContext<android.app.Application>().filesDir
            .resolve("seasons/S1").deleteRecursively()
        val vm = com.mb.scrapbook.lottery.ui.drawnight.DrawNightViewModel(
            ApplicationProvider.getApplicationContext()
        )
        // 等 store 加载完成(Idle 带 note)
        awaitIdle(vm)
        vm.startPeriod()
        awaitLocked(vm)

        assertTrue("应拒绝重复红球", vm.confirmEntry(listOf(1, 1, 11, 22, 28, 33), 9)!!.contains("重复"))
        assertEquals(null, vm.confirmEntry(listOf(1, 5, 11, 22, 28, 33), 9))
    }

    private fun awaitIdle(vm: com.mb.scrapbook.lottery.ui.drawnight.DrawNightViewModel) {
        await("idle") { vm.ui.value is com.mb.scrapbook.lottery.ui.drawnight.DrawNightViewModel.Ui.Idle && (vm.ui.value as com.mb.scrapbook.lottery.ui.drawnight.DrawNightViewModel.Ui.Idle).nextPeriod != "…" }
    }

    private fun awaitLocked(vm: com.mb.scrapbook.lottery.ui.drawnight.DrawNightViewModel) {
        await("locked") { vm.ui.value is com.mb.scrapbook.lottery.ui.drawnight.DrawNightViewModel.Ui.Locked }
    }

    private fun await(what: String, cond: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 30_000
        while (!cond()) {
            if (System.currentTimeMillis() > deadline) throw AssertionError("等待 $what 超时")
            Thread.sleep(100)
        }
    }
}
