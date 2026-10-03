package com.mb.scrapbook.lottery.infer

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

/**
 * llama.cpp JNI 封装(设计 R7,移植自 MiniCPM-V-demo-Android LlamaEngine,裁剪至竞技场所需):
 * 保留 原生加载/模型装卸/mmproj/系统提示词/流式生成/图像预填;去掉 demo 的竞速下载、
 * TTS/omni、视频帧、偏好存储。下载与校验在 [ModelDownloader](D13),槽位调度在 [ModelManager](A4)。
 * JNI 符号已重绑定到本包(llama_jni.cpp,构建脚本见 cpp/ 目录注释)。
 */
class LlamaEngine private constructor(
    private val nativeLibDir: String,
) {

    sealed class State {
        object Uninitialized : State()
        object Initializing : State()
        object Initialized : State()
        object LoadingModel : State()
        object ModelReady : State()
        object Generating : State()
        object UnloadingModel : State()
        data class Error(val exception: Exception) : State()
    }

    companion object {
        private val TAG = LlamaEngine::class.java.simpleName

        const val DEFAULT_PREDICT_LENGTH = 1024
        const val MAX_IMAGE_SLICE = 9
        const val DEFAULT_IMAGE_SLICE = MAX_IMAGE_SLICE

        @Volatile
        private var instance: LlamaEngine? = null

        fun getInstance(context: Context): LlamaEngine =
            instance ?: synchronized(this) {
                val nativeLibDir = context.applicationInfo.nativeLibraryDir
                require(nativeLibDir.isNotBlank()) { "Expected a valid native library path!" }
                LlamaEngine(nativeLibDir).also { instance = it }
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val llamaDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val llamaScope = CoroutineScope(llamaDispatcher + SupervisorJob())

    private val _state = MutableStateFlow<State>(State.Uninitialized)
    val state: StateFlow<State> = _state.asStateFlow()

    @Volatile
    private var _cancelGeneration = false

    @Volatile
    private var _readyForSystemPrompt = false

    @Volatile
    private var _mmprojLoaded = false

    // ---- JNI(符号于 cpp/llama_jni.cpp 重绑定为 com_mb_scrapbook_lottery_infer_LlamaEngine_*) ----
    private external fun init(nativeLibDir: String)
    private external fun load(modelPath: String): Int
    private external fun loadMmproj(mmprojPath: String, imageMaxSliceNums: Int): Int
    private external fun setImageMaxSliceNumsNative(n: Int)
    private external fun setMinicpmvVersionNative(version: Int)
    private external fun setEnableThinkingNative(enable: Boolean)
    private external fun setTemperatureNative(temp: Float)
    private external fun getMinicpmvVersionNative(): Int
    private external fun prepare(): Int
    private external fun systemInfo(): String
    private external fun processSystemPrompt(systemPrompt: String): Int
    private external fun processUserPrompt(userPrompt: String, predictLength: Int): Int
    private external fun generateNextToken(): String?
    private external fun prefillImage(imageData: ByteArray, imageSize: Int): Int
    private external fun fullReset()
    private external fun nativeCancelGeneration()
    private external fun unload()
    private external fun shutdown()

    init {
        llamaScope.launch {
            try {
                check(_state.value is State.Uninitialized) { "Cannot load native library in ${_state.value.javaClass.simpleName}!" }
                _state.value = State.Initializing
                System.loadLibrary("minicpm_v_demo")
                init(nativeLibDir)
                _state.value = State.Initialized
                Log.i(TAG, "Native library loaded! System info: \n${systemInfo()}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load native library", e)
                _state.value = State.Error(e)
                throw e
            }
        }
    }

    val isVisionSupported: Boolean get() = _mmprojLoaded

    /**
     * 装载模型。[minicpmvVersion] 仅 V-4.6 传 46(原生端 n_ctx/prefix 判定用),纯文本传 0。
     */
    suspend fun loadModel(pathToModel: String, pathToMmproj: String? = null, minicpmvVersion: Int = 0) =
        withContext(llamaDispatcher) {
            check(_state.value is State.Initialized) { "Cannot load model in ${_state.value.javaClass.simpleName}!" }
            try {
                File(pathToModel).let {
                    require(it.exists()) { "File not found: $pathToModel" }
                    require(it.isFile) { "Not a valid file: $pathToModel" }
                    require(it.canRead()) { "Cannot read file: $pathToModel" }
                }
                _readyForSystemPrompt = false
                _state.value = State.LoadingModel
                load(pathToModel).let { if (it != 0) throw RuntimeException("Failed to load model (code: $it)") }

                if (pathToMmproj != null) {
                    File(pathToMmproj).let {
                        require(it.exists()) { "mmproj file not found: $pathToMmproj" }
                        require(it.canRead()) { "Cannot read mmproj file: $pathToMmproj" }
                    }
                    loadMmproj(pathToMmproj, DEFAULT_IMAGE_SLICE).let {
                        if (it != 0) {
                            Log.w(TAG, "Failed to load mmproj (code: $it), continuing without vision support")
                        } else {
                            _mmprojLoaded = true
                            setMinicpmvVersionNative(minicpmvVersion)
                        }
                    }
                }

                prepare().let { if (it != 0) throw RuntimeException("Failed to prepare resources (code: $it)") }
                _readyForSystemPrompt = true
                _cancelGeneration = false
                _state.value = State.ModelReady
            } catch (e: Exception) {
                Log.e(TAG, (e.message ?: "Error loading model") + "\n" + pathToModel, e)
                _state.value = State.Error(e)
                throw e
            }
        }

    suspend fun setSystemPrompt(prompt: String) =
        withContext(llamaDispatcher) {
            require(prompt.isNotBlank()) { "Cannot process empty system prompt!" }
            check(_readyForSystemPrompt) { "System prompt must be set RIGHT AFTER model loaded!" }
            _readyForSystemPrompt = false
            processSystemPrompt(prompt).let { result ->
                if (result != 0) {
                    RuntimeException("Failed to process system prompt: $result").also {
                        _state.value = State.Error(it)
                        throw it
                    }
                }
            }
            _state.value = State.ModelReady
        }

    /** VLM 读图:预填图像字节,随后走 [sendUserPrompt] 提问。 */
    suspend fun prefillImage(imageData: ByteArray) =
        withContext(llamaDispatcher) {
            check(_mmprojLoaded) { "Vision model not loaded!" }
            check(_state.value is State.ModelReady) { "Cannot prefill image in ${_state.value.javaClass.simpleName}!" }
            val result = prefillImage(imageData, imageData.size)
            if (result != 0) throw RuntimeException("Failed to prefill image (code: $result)")
        }

    /** 流式生成:逐 token 发出(LLM persona 出票的流式理由通道,R7)。 */
    fun sendUserPrompt(
        message: String,
        predictLength: Int = DEFAULT_PREDICT_LENGTH,
    ): Flow<String> = flow {
        require(message.isNotEmpty()) { "User prompt must not be empty!" }
        check(_state.value is State.ModelReady) { "User prompt discarded due to: ${_state.value.javaClass.simpleName}" }
        try {
            _cancelGeneration = false
            _readyForSystemPrompt = false
            _state.value = State.Generating
            processUserPrompt(message, predictLength).let { if (it != 0) return@flow }
            while (!_cancelGeneration) {
                generateNextToken()?.let { utf8token ->
                    if (utf8token.isNotEmpty()) emit(utf8token)
                } ?: break
            }
            _state.value = State.ModelReady
        } catch (e: CancellationException) {
            _state.value = State.ModelReady
            throw e
        } catch (e: Exception) {
            _state.value = State.Error(e)
            throw e
        }
    }.flowOn(llamaDispatcher)

    /** D23 persona 温度阶梯(0.3/1.0/1.3):即时重建采样器,不重载模型。 */
    suspend fun setTemperature(temp: Float) = withContext(llamaDispatcher) {
        setTemperatureNative(temp)
    }

    fun cancelGeneration() {
        _cancelGeneration = true
        llamaScope.launch { nativeCancelGeneration() }
    }

    /** A4 内存纪律:换槽先卸载。 */
    suspend fun unloadModel() = withContext(llamaDispatcher) {
        if (_state.value is State.ModelReady) {
            _readyForSystemPrompt = false
            _mmprojLoaded = false
            _state.value = State.UnloadingModel
            unload()
            _state.value = State.Initialized
        }
    }

    fun destroy() {
        _cancelGeneration = true
        runBlocking(llamaDispatcher) {
            _readyForSystemPrompt = false
            _mmprojLoaded = false
            when (_state.value) {
                is State.Uninitialized -> {}
                is State.Initialized -> shutdown()
                else -> {
                    unload()
                    shutdown()
                }
            }
        }
        llamaScope.cancel()
    }
}
