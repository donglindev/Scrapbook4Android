package com.mb.scrapbook.lottery.ui.settings

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.mb.scrapbook.lib.base.mvvm.view.BaseActivity
import com.mb.scrapbook.lottery.R
import com.mb.scrapbook.lottery.data.AppPrefs
import com.mb.scrapbook.lottery.data.SeasonStore
import com.mb.scrapbook.lottery.data.SideloadGuard
import com.mb.scrapbook.lottery.databinding.ActivitySettingsBinding
import com.mb.scrapbook.lottery.ui.LotteryMainActivity
import com.mb.scrapbook.lottery.ui.models.ModelDownloadActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** 赛季管理(D7=A):侧载导入(T9)/ 重建 / 归档;切换为单赛季 MVP 占位(多赛季后启用)。 */
class SettingsActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val store by lazy { SeasonStore(applicationContext, lifecycleScope) }

    private val pickCsv = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            val csv = withContext(Dispatchers.IO) {
                contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            } ?: return@launch
            store.load { com.mb.scrapbook.lottery.core.data.DrawRepository().load() }
            val result = withContext(Dispatchers.IO) { runCatching { store.importSideload(csv) } }
            result.fold(
                onSuccess = { r ->
                    AlertDialog.Builder(this@SettingsActivity)
                        .setTitle("侧载完成(D14)")
                        .setMessage(
                            buildString {
                                append("追加新期 ${r.newPeriods.size} 期\n")
                                if (r.conflicts.isEmpty()) append("无冲突。")
                                else append("冲突 ${r.conflicts.size} 条(已存数据与账本未改动):\n${r.conflicts.joinToString("\n")}")
                            }
                        )
                        .setPositiveButton("好", null)
                        .show()
                },
                onFailure = { e ->
                    AlertDialog.Builder(this@SettingsActivity)
                        .setTitle("侧载失败")
                        .setMessage(e.message ?: "解析失败")
                        .setPositiveButton("好", null)
                        .show()
                },
            )
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_settings

    override fun onInitView() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings)
    }

    override fun onInitData() {
        binding.rowSideload.setOnClickListener { pickCsv.launch("*/*") }
        binding.rowModels.setOnClickListener { startActivity(Intent(this, ModelDownloadActivity::class.java)) }
        binding.rowRebuild.setOnClickListener { confirm("重建赛季", "清空 S1 全部票据与账本(不可恢复),确认?") { rebuildSeason() } }
        binding.rowArchive.setOnClickListener { confirm("归档赛季", "当前赛季移入归档并开新 S1,确认?") { archiveSeason() } }
        binding.rowSwitch.isEnabled = false // 单赛季 MVP:归档后经向导重开即新赛季
        binding.rowSwitch.alpha = 0.5f
    }

    private fun confirm(title: String, message: String, action: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("确认") { _, _ -> action() }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun rebuildSeason() = lifecycleScope.launch {
        withContext(Dispatchers.IO) {
            File(applicationContext.filesDir, "seasons/S1").deleteRecursively()
            AppPrefs.clearSeasonStart(applicationContext)
        }
        restartMain()
    }

    private fun archiveSeason() = lifecycleScope.launch {
        withContext(Dispatchers.IO) {
            val dir = File(applicationContext.filesDir, "seasons/S1")
            if (dir.exists()) {
                val stamp = java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.US)
                    .format(java.util.Date())
                dir.renameTo(File(dir.parentFile, "S1-archived-$stamp"))
            }
            AppPrefs.clearSeasonStart(applicationContext)
        }
        restartMain()
    }

    private fun restartMain() {
        startActivity(
            Intent(this, LotteryMainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        finish()
    }
}
