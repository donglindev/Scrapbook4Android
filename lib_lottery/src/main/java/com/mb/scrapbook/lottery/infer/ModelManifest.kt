package com.mb.scrapbook.lottery.infer

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * 模型下载清单(D25:打包 APK assets,发版即换)。来源事实 = MiniCPM-V-demo ModelInfo
 * (HF 官方 repo + hf-mirror 镜像 + ModelScope 兜底,依序尝试)。
 * checksum:已知则校验(MD5,来自官方发布);SHA-256 待真机首下后回填发布(D13 完全体)。
 * sizeBytes 为空时 UI 显示"约 1.5GB"级估算文案。
 */
data class ModelManifest(
    val slots: List<ModelSlot>,
) {
    data class ModelSlot(
        val id: String,
        val displayName: String,
        /** V-4.6 传 46,纯文本 0(LlamaEngine 装载参数)。 */
        val minicpmvVersion: Int = 0,
        val files: List<ModelFile>,
    ) {
        val isVision: Boolean get() = files.any { it.isMmproj }
    }

    data class ModelFile(
        val name: String,
        val urls: List<String>,
        val sizeBytes: Long? = null,
        val checksum: Checksum? = null,
        val isMmproj: Boolean = false,
    ) {
        data class Checksum(val algorithm: String, val value: String)
    }

    companion object {
        private const val ASSET = "lottery_models.json"
        private val gson = Gson()

        /** 信任边界:Gson 无构造器校验,读入后手工校验。 */
        fun load(context: Context): ModelManifest {
            val json = context.assets.open(ASSET).bufferedReader().use { it.readText() }
            val manifest = gson.fromJson<ModelManifest>(json, object : TypeToken<ModelManifest>() {}.type)
            require(manifest?.slots?.isNotEmpty() == true) { "模型清单为空: $ASSET" }
            manifest.slots.forEach { slot ->
                require(slot.id.isNotBlank() && slot.displayName.isNotBlank()) { "槽位 id/displayName 缺失" }
                require(slot.files.isNotEmpty()) { "槽位 ${slot.id} 无文件" }
                slot.files.forEach { f ->
                    require(f.name.isNotBlank() && f.urls.isNotEmpty()) { "槽位 ${slot.id} 文件 ${f.name} 无下载源" }
                }
            }
            return manifest
        }

        fun dirFor(context: Context, slotId: String): File =
            File(File(context.filesDir, "models"), slotId).apply { mkdirs() }

        fun fileFor(context: Context, slot: ModelSlot, file: ModelFile): File =
            File(dirFor(context, slot.id), file.name)

        /** 槽位全部文件就位(下载页"就绪"判定)。 */
        fun slotReady(context: Context, slot: ModelSlot): Boolean =
            slot.files.all { fileFor(context, slot, it).exists() }
    }
}
