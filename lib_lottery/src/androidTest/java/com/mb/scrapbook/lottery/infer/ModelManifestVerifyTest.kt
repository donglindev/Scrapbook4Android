package com.mb.scrapbook.lottery.infer

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest

/** T10-5 模型清单校验(D13):assets 解析、就绪判定、小文件假模型走真校验(本地 file:// 源)。 */
@RunWith(AndroidJUnit4::class)
class ModelManifestVerifyTest {

    private val context get() = ApplicationProvider.getApplicationContext<android.app.Application>()

    @Test
    fun manifestLoadsAndValidates() {
        val manifest = ModelManifest.load(context)
        assertEquals(2, manifest.slots.size)
        assertTrue(manifest.slots.any { it.id == "minicpm5-2b" && !it.isVision })
        assertTrue(manifest.slots.any { it.id == "minicpm-v-4.6" && it.isVision })
    }

    @Test
    fun slotReadyReflectsFiles() {
        val manifest = ModelManifest.load(context)
        val slot = manifest.slots.first { it.id == "minicpm5-2b" }
        val dir = ModelManifest.dirFor(context, slot.id)
        dir.deleteRecursively()
        assertFalse(ModelManifest.slotReady(context, slot))
        // 假模型:小文件就位 → ready(内容校验在下载器,就绪判定只看存在性)
        File(dir, slot.files.single().name).writeBytes(byteArrayOf(1, 2, 3))
        assertTrue(ModelManifest.slotReady(context, slot))
        dir.deleteRecursively()
    }

    /** D13 全链路:校验失败 → 删除 → 自动重下一次 → 再失败报错(本地 file:// 双源)。 */
    @Test
    fun downloaderVerifiesAndRetriesThenFails() {
        val bad = File(context.cacheDir, "fake-model.gguf").apply { writeBytes(byteArrayOf(9, 9, 9)) }
        val file = ModelManifest.ModelFile(
            name = "verified-fake.gguf",
            urls = listOf(bad.toURI().toString(), bad.toURI().toString()),
            checksum = ModelManifest.ModelFile.Checksum("MD5", "deadbeefdeadbeefdeadbeefdeadbeef"),
        )
        val target = File(context.cacheDir, "slot-test")
        target.deleteRecursively(); target.mkdirs()
        val e = assertThrows(RuntimeException::class.java) {
            runBlocking { ModelDownloader().downloadSlot(target, file).toList() }
        }
        assertTrue(e.message!!.contains("校验失败"))
        assertFalse(File(target, "verified-fake.gguf").exists()) // 坏文件已删
    }

    @Test
    fun downloaderAcceptsMatchingChecksum() {
        val bytes = "hello-arena".toByteArray()
        val md5 = MessageDigest.getInstance("MD5").digest(bytes).joinToString("") { "%02x".format(it) }
        val good = File(context.cacheDir, "good-model.gguf").apply { writeBytes(bytes) }
        val file = ModelManifest.ModelFile(
            name = "verified-good.gguf",
            urls = listOf(good.toURI().toString()),
            checksum = ModelManifest.ModelFile.Checksum("MD5", md5),
        )
        val target = File(context.cacheDir, "slot-test-ok")
        target.deleteRecursively(); target.mkdirs()
        runBlocking { ModelDownloader().downloadSlot(target, file).toList() }
        assertTrue(File(target, "verified-good.gguf").exists())
    }
}
