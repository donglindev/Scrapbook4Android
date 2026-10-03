package com.mb.scrapbook.lottery.data

import android.content.Context
import com.google.gson.Gson
import com.mb.scrapbook.lottery.core.arena.PeriodResult
import com.mb.scrapbook.lottery.core.domain.Draw
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 赛季持久化(R7):filesDir/seasons/S1/ 下按期 JSONL(results + draws)。
 * 内存态同步更新(结算后立即可读),文件写走 IO 协程;读入走信任边界校验。
 */
class SeasonStore(context: Context, private val scope: kotlinx.coroutines.CoroutineScope) {

    private val dir = File(context.filesDir, "seasons/S1").apply { mkdirs() }
    private val resultsFile = File(dir, "results.jsonl")
    private val drawsFile = File(dir, "draws.jsonl")
    private val sideloadFile = File(dir, "sideload.csv")
    private val gson = Gson()

    val archive = mutableListOf<PeriodResult>()
    val settledDraws = mutableListOf<Draw>()
    var baseHistory: List<Draw> = emptyList()
        private set

    /** 首次进入:读侧载副本(若有,否则打包历史 classpath 单源)+ 已结算归档。 */
    suspend fun load(base: () -> List<Draw>) {
        withContext(Dispatchers.IO) {
            baseHistory = base()
            if (sideloadFile.exists()) {
                runCatching { baseHistory = SideloadGuard.parseCsv(sideloadFile.readText()) }
                    .onFailure { sideloadFile.delete() } // 侧载副本损坏 → 回退打包源并清毒
            }
            if (resultsFile.exists()) {
                resultsFile.readLines().filter { it.isNotBlank() }.forEach { archive += PeriodResult.fromJson(it) }
            }
            if (drawsFile.exists()) {
                drawsFile.readLines().filter { it.isNotBlank() }.forEach { line ->
                    val d = gson.fromJson(line, Draw::class.java)
                    settledDraws += Draw(d.period, d.reds, d.blue, d.date, d.source) // 重查不变量(Gson 绕过构造器)
                }
            }
        }
    }

    /**
     * 侧载刷新(D14):冲突告警 + 只追加新期;已结算期绝不回改。
     * 侧载副本 = 现有 base + 新期(不含已结算 live 期,避免与 settled 重复)。
     */
    suspend fun importSideload(csv: String): SideloadGuard.Result {
        val incoming = SideloadGuard.parseCsv(csv)
        val settledPeriods = settledDraws.map { it.period }.toSet()
        val merged = SideloadGuard.merge(history(), settledPeriods, incoming)
        val newBasePeriods = merged.newPeriods.filter { it.period !in settledPeriods }
        withContext(Dispatchers.IO) {
            sideloadFile.writeText(SideloadGuard.toCsv(baseHistory + newBasePeriods))
        }
        baseHistory = baseHistory + newBasePeriods
        return merged.copy(newPeriods = newBasePeriods)
    }

    /** 出票可见历史 = 打包历史 + 已结算 live 期(live-forward 向前延伸)。 */
    fun history(): List<Draw> = baseHistory + settledDraws

    fun nextPeriod(): String {
        val last = settledDraws.lastOrNull()?.period ?: baseHistory.lastOrNull()?.period ?: "00000"
        return "%05d".format(last.toInt() + 1)
    }

    /** 内存同步追加(结算后立即可读);文件异步落盘。 */
    fun append(draw: Draw, results: List<PeriodResult>) {
        archive += results
        settledDraws += draw
        scope.launch(Dispatchers.IO) {
            resultsFile.appendText(results.joinToString("") { PeriodResult.toJson(it) + "\n" })
            drawsFile.appendText(gson.toJson(draw) + "\n")
        }
    }
}
