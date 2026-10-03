package com.mb.scrapbook.lottery.infer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * 模型下载器(D13=A 改造移植):断点续传(.tmp + Range)、多源顺序兜底(HF→镜像→ModelScope)、
 * 校验失败删除并自动重下一次、再失败才报错。进度经 Flow 推给下载页(TD2)。
 */
class ModelDownloader {

    sealed class Progress {
        data class Downloading(val file: String, val bytes: Long, val totalBytes: Long, val sourceIndex: Int, val sourceCount: Int) : Progress()
        data class Verifying(val file: String) : Progress()
        data class FileDone(val file: String, val mb: Long) : Progress()
    }

    /** 下载槽位全部文件;校验失败自动重下一次(D13)。 */
    fun downloadSlot(
        dir: File,
        file: ModelManifest.ModelFile,
    ): Flow<Progress> = flow {
        val target = File(dir, file.name)
        val tmp = File(dir, "${file.name}.tmp")

        var attempt = 0
        while (true) {
            attempt++
            try {
                downloadOne(target, tmp, file, attempt > 1, emit = { emit(it) })
                emit(Progress.FileDone(file.name, target.length() / (1024 * 1024)))
                return@flow
            } catch (e: ChecksumMismatchException) {
                target.delete()
                tmp.delete()
                if (attempt >= 2) throw RuntimeException("校验失败(已自动重下 1 次): ${file.name} ${e.message}", e)
                // D13:删除重下
            }
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun downloadOne(
        target: File,
        tmp: File,
        file: ModelManifest.ModelFile,
        fresh: Boolean,
        emit: suspend (Progress) -> Unit,
    ) {
        if (fresh) tmp.delete()

        val resumeFrom = if (tmp.exists()) tmp.length() else 0L
        var lastError: Exception? = null
        var conn: HttpURLConnection? = null
        for ((index, urlStr) in file.urls.withIndex()) {
            try {
                conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 120_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "SSQ-Arena/1.0")
                    if (resumeFrom > 0) setRequestProperty("Range", "bytes=$resumeFrom-")
                }
                val code = conn.responseCode
                if (code !in 200..206) throw RuntimeException("HTTP $code ($urlStr)")

                val acceptedResume = code == HttpURLConnection.HTTP_PARTIAL
                if (code == HttpURLConnection.HTTP_OK && resumeFrom > 0) tmp.delete() // 服务器不支持 Range,重来
                val start = if (acceptedResume) resumeFrom else 0L
                val remaining = conn.contentLength.toLong()
                val total = if (remaining > 0) remaining + start else -1L

                conn.inputStream.use { input ->
                    FileOutputStream(tmp, acceptedResume && resumeFrom > 0).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var read = start
                        while (true) {
                            val n = input.read(buffer)
                            if (n == -1) break
                            output.write(buffer, 0, n)
                            read += n
                            emit(Progress.Downloading(file.name, read, total, index + 1, file.urls.size))
                        }
                        output.flush()
                        try { output.fd.sync() } catch (_: Throwable) {}
                    }
                }

                if (target.exists()) target.delete()
                check(tmp.renameTo(target) || run { tmp.copyTo(target, overwrite = true); tmp.delete(); true }) { "落盘失败: ${file.name}" }

                file.checksum?.let { ck ->
                    emit(Progress.Verifying(file.name))
                    val actual = digest(target, ck.algorithm)
                    if (!actual.equals(ck.value, ignoreCase = true)) {
                        throw ChecksumMismatchException("${ck.algorithm} 期望 ${ck.value} 实得 $actual")
                    }
                }
                return
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                conn?.disconnect()
                conn = null
                // 尝试下一个源
            }
        }
        throw RuntimeException("全部 ${file.urls.size} 个下载源失败: ${file.name}", lastError)
    }

    private class ChecksumMismatchException(message: String) : RuntimeException(message)

    private fun digest(file: File, algorithm: String): String {
        val md = MessageDigest.getInstance(algorithm.uppercase())
        FileInputStream(file).use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n <= 0) break
                md.update(buffer, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
