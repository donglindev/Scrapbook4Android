package com.mb.scrapbook.lottery.ui.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mb.scrapbook.lottery.infer.ModelDownloader
import com.mb.scrapbook.lottery.infer.ModelManager
import com.mb.scrapbook.lottery.infer.ModelManifest
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** 模型下载页 VM(D8=A 状态机):未开始 → 下载中 → 校验中 → 失败(红条) → 就绪。 */
class ModelDownloadViewModel(app: Application) : AndroidViewModel(app) {

    data class SlotUi(
        val slotId: String,
        val displayName: String,
        val stateText: String,
        /** 0-100;-1 = 不确定长度。 */
        val progressPercent: Int = -1,
        val bytesText: String = "",
        val error: String? = null,
        val downloading: Boolean = false,
        val ready: Boolean = false,
    )

    private val manager = ModelManager.getInstance(app)
    val deviceSupported: Boolean get() = manager.deviceSupported
    val slots: List<ModelManifest.ModelSlot> get() = manager.manifest.slots

    private val _slotUi = MutableLiveData<Map<String, SlotUi>>(emptyMap())
    val slotUi: LiveData<Map<String, SlotUi>> = _slotUi
    private val jobs = HashMap<String, Job>()

    init {
        refresh()
    }

    fun refresh() {
        _slotUi.value = slots.associate { slot ->
            slot.id to slot.ui(manager.slotState(slot.id))
        }
    }

    private fun ModelManifest.ModelSlot.ui(state: ModelManager.SlotState): SlotUi = when (state) {
        ModelManager.SlotState.NotStarted -> SlotUi(id, displayName, "未开始")
        ModelManager.SlotState.Downloading -> SlotUi(id, displayName, "下载中", downloading = true)
        ModelManager.SlotState.Verifying -> SlotUi(id, displayName, "校验中", downloading = true)
        is ModelManager.SlotState.Failed -> SlotUi(id, displayName, "下载失败", error = state.reason)
        ModelManager.SlotState.Ready -> SlotUi(id, displayName, "已就绪(开奖夜自动装载)", ready = true)
        ModelManager.SlotState.Loaded -> SlotUi(id, displayName, "常驻中(A4)", ready = true)
    }

    fun download(slotId: String) {
        if (jobs[slotId]?.isActive == true) return
        val slot = slots.first { it.id == slotId }
        jobs[slotId] = viewModelScope.launch {
            update(slotId) { it.copy(stateText = "下载中", downloading = true, error = null, progressPercent = -1) }
            val dir = ModelManifest.dirFor(getApplication(), slot.id)
            try {
                for (file in slot.files) {
                    ModelDownloader().downloadSlot(dir, file).collect { p ->
                        when (p) {
                            is ModelDownloader.Progress.Downloading -> update(slotId) {
                                val pct = if (p.totalBytes > 0) (p.bytes * 100 / p.totalBytes).toInt() else -1
                                it.copy(
                                    stateText = "下载中(源 ${p.sourceIndex}/${p.sourceCount})",
                                    progressPercent = pct,
                                    bytesText = "${p.bytes / (1024L * 1024L)} MB",
                                    downloading = true,
                                )
                            }
                            is ModelDownloader.Progress.Verifying -> update(slotId) {
                                it.copy(stateText = "校验中(${p.file})", progressPercent = 100)
                            }
                            is ModelDownloader.Progress.FileDone -> update(slotId) {
                                it.copy(stateText = "已完成 ${p.file}(${p.mb} MB)")
                            }
                        }
                    }
                }
                update(slotId) { SlotUi(slotId, slot.displayName, "已就绪(开奖夜自动装载)", ready = true) }
            } catch (e: Exception) {
                update(slotId) { it.copy(stateText = "下载失败", downloading = false, error = e.message ?: "未知错误") }
            }
            manager.refreshStates()
        }
    }

    private fun update(slotId: String, transform: (SlotUi) -> SlotUi) {
        _slotUi.value = (_slotUi.value ?: emptyMap()).toMutableMap().apply {
            val current = get(slotId) ?: SlotUi(slotId, slotId, "未开始")
            put(slotId, transform(current))
        }
    }
}
