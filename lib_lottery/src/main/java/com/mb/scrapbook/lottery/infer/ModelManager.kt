package com.mb.scrapbook.lottery.infer

import android.content.Context
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 模型槽位管理(A4 内存纪律:同一时刻至多一个模型常驻;LLM ×3 persona 复用同一引擎,换 persona 只换 prompt)。
 * 槽位状态机供下载页(TD2)与策略接入(T8)消费。
 */
class ModelManager private constructor(private val context: Context) {

    /** D16=A:非 arm64 设备 requiresModel 策略显示「设备不支持」而非崩溃。 */
    val deviceSupported: Boolean = Build.SUPPORTED_ABIS.contains("arm64-v8a")

    sealed class SlotState {
        object NotStarted : SlotState()
        object Downloading : SlotState()
        object Verifying : SlotState()
        data class Failed(val reason: String) : SlotState()
        object Ready : SlotState()
        object Loaded : SlotState() // 常驻中(A4)
    }

    private val _slotStates = MutableStateFlow<Map<String, SlotState>>(emptyMap())
    val slotStates: StateFlow<Map<String, SlotState>> = _slotStates.asStateFlow()

    private val downloader = ModelDownloader()
    val manifest: ModelManifest by lazy { ModelManifest.load(context) }

    /** 当前常驻槽位 id(null = 无,A4)。 */
    @Volatile
    var residentSlotId: String? = null
        private set

    fun slotState(slotId: String): SlotState =
        _slotStates.value[slotId] ?: if (ModelManifest.slotReady(context, manifest.slots.first { it.id == slotId })) SlotState.Ready else SlotState.NotStarted

    fun refreshStates() {
        _slotStates.value = manifest.slots.associate { slot ->
            slot.id to (slotState(slot.id).let { if (slot.id == residentSlotId) SlotState.Loaded else it })
        }
    }

    /**
     * 装载槽位(A4:先卸载现有常驻)。suspend 直到 ModelReady 或抛错。
     * 下载未就绪时抛 IllegalStateException。
     */
    suspend fun loadSlot(slotId: String): LlamaEngine {
        val slot = manifest.slots.firstOrNull { it.id == slotId }
            ?: throw IllegalArgumentException("未知槽位: $slotId")
        check(deviceSupported) { "设备不支持(需 arm64-v8a,D16)" }
        check(ModelManifest.slotReady(context, slot)) { "模型未就绪,请先下载: $slotId" }

        val engine = LlamaEngine.getInstance(context)
        residentSlotId?.takeIf { it != slotId }?.let { engine.unloadModel() } // A4 分时加载
        val gguf = slot.files.first { !it.isMmproj }
        val mmproj = slot.files.firstOrNull { it.isMmproj }
        engine.loadModel(
            ModelManifest.fileFor(context, slot, gguf).absolutePath,
            mmproj?.let { ModelManifest.fileFor(context, slot, it).absolutePath },
            minicpmvVersion = slot.minicpmvVersion,
        )
        residentSlotId = slotId
        refreshStates()
        return engine
    }

    suspend fun unloadResident() {
        residentSlotId?.let {
            LlamaEngine.getInstance(context).unloadModel()
        }
        residentSlotId = null
        refreshStates()
    }

    companion object {
        @Volatile
        private var instance: ModelManager? = null

        fun getInstance(context: Context): ModelManager =
            instance ?: synchronized(this) {
                ModelManager(context.applicationContext).also { instance = it }
            }
    }
}
