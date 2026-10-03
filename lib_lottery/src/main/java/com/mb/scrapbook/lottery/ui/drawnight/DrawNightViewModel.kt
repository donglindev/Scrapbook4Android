package com.mb.scrapbook.lottery.ui.drawnight

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mb.scrapbook.lottery.core.arena.LedgerAggregator
import com.mb.scrapbook.lottery.core.arena.PeriodResult
import com.mb.scrapbook.lottery.core.arena.SeasonRunner
import com.mb.scrapbook.lottery.core.data.DrawRepository
import com.mb.scrapbook.lottery.core.domain.BetPlan
import com.mb.scrapbook.lottery.core.domain.Draw
import com.mb.scrapbook.lottery.core.domain.Verdict
import com.mb.scrapbook.lottery.core.strategy.ColdHotStrategy
import com.mb.scrapbook.lottery.core.strategy.FrequencyStrategy
import com.mb.scrapbook.lottery.core.strategy.MatrixStrategy
import com.mb.scrapbook.lottery.core.strategy.MissingStrategy
import com.mb.scrapbook.lottery.core.strategy.PickContext
import com.mb.scrapbook.lottery.core.strategy.PixelSeedStrategy
import com.mb.scrapbook.lottery.core.strategy.RandomStrategy
import com.mb.scrapbook.lottery.core.strategy.Strategy
import com.mb.scrapbook.lottery.data.SeasonStore
import com.mb.scrapbook.lottery.infer.LlamaEngine
import com.mb.scrapbook.lottery.infer.ModelManager
import com.mb.scrapbook.lottery.strategy.LlmPersonaPicker
import com.mb.scrapbook.lottery.strategy.Persona
import com.mb.scrapbook.lottery.strategy.Personas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * 开奖夜状态机(D6=A 三态:出票→锁定→结算)。JVM 六选手 + LLM persona(T8,D23 三人格,
 * 模型就绪时加入;流式理由上聚光灯)。settle gate(D12)/不可变(D14)/弃权 0 注(R5)由 core 保证。
 */
class DrawNightViewModel(app: Application) : AndroidViewModel(app) {

    /** 参赛选手:JVM 直接出票;LLM 走流式 persona。 */
    private sealed class Player {
        abstract val id: String
        abstract val displayName: String

        class Jvm(val strategy: Strategy) : Player() {
            override val id = strategy.id
            override val displayName = strategy.displayName
        }

        class Llm(val persona: Persona) : Player() {
            override val id = persona.id
            override val displayName = persona.displayName
        }
    }

    private val store = SeasonStore(getApplication(), viewModelScope)
    private val modelManager = ModelManager.getInstance(getApplication())
    private val jvmPlayers = listOf(
        Player.Jvm(RandomStrategy()),
        Player.Jvm(FrequencyStrategy()),
        Player.Jvm(ColdHotStrategy()),
        Player.Jvm(MissingStrategy()),
        Player.Jvm(MatrixStrategy()),
        Player.Jvm(PixelSeedStrategy()),
    )
    private var players: List<Player> = jvmPlayers
    private val llmSlotId = "minicpm5-2b"

    sealed class Ui {
        data class Idle(val nextPeriod: String, val settledCount: Int, val note: String) : Ui()
        data class Picking(
            val index: Int,
            val total: Int,
            val name: String,
            val streamingText: String,
            val doneTickets: List<Pair<String, String>>,
        ) : Ui()

        data class Locked(val period: String, val tickets: List<Pair<String, String>>) : Ui()
        data class Resulted(val period: String, val rows: List<ResultRow>) : Ui()
    }

    data class ResultRow(
        val strategyId: String,
        val name: String,
        val ticketsText: String,
        val hitText: String,
        val winningsYuan: Long,
        val verdict: Verdict = Verdict.INSUFFICIENT_SAMPLE,
    )

    private val _ui = MutableLiveData<Ui>(Ui.Idle("…", 0, "赛季装载中…"))
    val ui: LiveData<Ui> = _ui

    private var runner: SeasonRunner? = null
    private var currentPeriod: String = ""

    init {
        viewModelScope.launch {
            store.load { DrawRepository().load() }
            store.startPeriodOverride = com.mb.scrapbook.lottery.data.AppPrefs.seasonStart(getApplication())
            players = buildPlayers()
            _ui.value = Ui.Idle(store.nextPeriod(), store.settledDraws.size, rosterNote())
        }
    }

    private fun buildPlayers(): List<Player> {
        val llmReady = modelManager.deviceSupported && runCatching {
            modelManager.manifest.slots.first { it.id == llmSlotId }
        }.map { slot ->
            com.mb.scrapbook.lottery.infer.ModelManifest.slotReady(getApplication(), slot)
        }.getOrDefault(false)
        return if (llmReady) jvmPlayers + Personas.ALL.map { Player.Llm(it) } else jvmPlayers
    }

    private fun rosterNote(): String = when {
        players.any { it is Player.Llm } -> "LLM 军师 ×3 已就绪(全阵容 ${players.size} 位)"
        !modelManager.deviceSupported -> "设备不支持 LLM(需 arm64-v8a,D16)· JVM ${players.size} 位出战"
        else -> "LLM 军师未就绪(模型未下载,TD2 下载页或侧载)· JVM ${players.size} 位出战"
    }

    /** 出票态:JVM 轮演 500ms/位;LLM persona 真流式(D10 聚光灯)。 */
    fun startPeriod() {
        if (_ui.value is Ui.Picking) return
        viewModelScope.launch {
            currentPeriod = store.nextPeriod()
            val runner = SeasonRunner(players.map { it.id }.toSet())
            val seedBase = "$currentPeriod|S1".hashCode().toLong()
            val done = mutableListOf<Pair<String, String>>()

            var engine: LlamaEngine? = null
            var picker: LlmPersonaPicker? = null
            if (players.any { it is Player.Llm }) {
                try {
                    val loaded = modelManager.loadSlot(llmSlotId)
                    loaded.setSystemPrompt(Personas.ARENA_SYSTEM_PROMPT)
                    engine = loaded
                    picker = LlmPersonaPicker(loaded)
                } catch (e: Exception) {
                    // 模型装载失败:LLM 全员按弃权入账(弃权也是实验数据),JVM 正常
                    players.filterIsInstance<Player.Llm>().forEach {
                        runner.recordAbstention(it.id, currentPeriod, "模型装载失败:${e.message}", System.currentTimeMillis())
                        done += it.displayName to "弃权(模型装载失败)"
                    }
                }
            }

            for ((i, p) in players.withIndex()) {
                when (p) {
                    is Player.Jvm -> {
                        _ui.value = Ui.Picking(i + 1, players.size, p.displayName, "思考中…", done.toList())
                        delay(500)
                        val plan = p.strategy.pick(
                            PickContext(currentPeriod, store.history(), Random(seedBase * 31L + i))
                        )
                        runner.recordPick(p.id, currentPeriod, plan, pickAt = System.currentTimeMillis())
                        done += p.displayName to ticketText(plan)
                    }
                    is Player.Llm -> {
                        if (picker == null || engine == null) continue // 已按弃权入账
                        synchronized(streamingBuf) { streamingBuf.setLength(0) }
                        _ui.value = Ui.Picking(i + 1, players.size, p.displayName, "思考中…", done.toList())
                        var lastEmit = 0L
                        val outcome = picker.pick(p.persona, currentPeriod, store.history()) { token ->
                            val now = System.currentTimeMillis()
                            if (now - lastEmit > 150) { // 流式节流
                                lastEmit = now
                                val text = synchronized(streamingBuf) {
                                    streamingBuf.append(token)
                                    streamingBuf.toString()
                                }.takeLast(200)
                                _ui.postValue(Ui.Picking(i + 1, players.size, p.displayName, text, done.toList()))
                            }
                        }
                        when (outcome) {
                            is LlmPersonaPicker.Outcome.Ticket -> {
                                runner.recordPick(
                                    p.id, currentPeriod, outcome.plan,
                                    pickAt = System.currentTimeMillis(),
                                    reason = outcome.reason,
                                    modelMeta = mapOf(
                                        "persona" to p.persona.displayName,
                                        "temperature" to p.persona.temperature.toString(),
                                    ),
                                )
                                done += p.displayName to ticketText(outcome.plan)
                            }
                            is LlmPersonaPicker.Outcome.Abstain -> {
                                runner.recordAbstention(p.id, currentPeriod, outcome.reason, System.currentTimeMillis())
                                done += p.displayName to "弃权(${outcome.reason.take(20)})"
                            }
                        }
                    }
                }
            }
            this@DrawNightViewModel.runner = runner
            _ui.value = Ui.Locked(currentPeriod, done)
        }
    }

    /** LLM 流式理由缓冲(每 persona 重置;只展示末 200 字)。 */
    private val streamingBuf = StringBuilder()

    /** D14:录入确认后结算。返回错误文案或 null(成功)。 */
    fun confirmEntry(reds: List<Int>, blue: Int): String? {
        com.mb.scrapbook.lottery.data.DrawEntryValidator.validate(reds, blue)?.let { return it }
        val r = runner ?: return "本期限未出票"
        val draw = Draw(
            currentPeriod,
            reds.sorted(),
            blue,
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
            "manual",
        )
        val results: List<PeriodResult> = try {
            r.settle(draw, enteredAt = System.currentTimeMillis())
        } catch (e: IllegalStateException) {
            return e.message ?: "结算失败"
        }
        store.append(draw, results)
        _ui.value = Ui.Resulted(currentPeriod, resultRows(results))
        refreshVerdicts()
        return null
    }

    fun nextPeriod() {
        _ui.value = Ui.Idle(store.nextPeriod(), store.settledDraws.size, rosterNote())
    }

    /** live 判决刷新(< 30 期全部「样本不足」,≥ 30 期 horizon null 判决)。 */
    private fun refreshVerdicts() {
        viewModelScope.launch(Dispatchers.Default) {
            val current = _ui.value as? Ui.Resulted ?: return@launch
            val judged = LedgerAggregator.judgeLive(store.archive, store.settledDraws)
            _ui.postValue(
                current.copy(
                    rows = current.rows.map {
                        it.copy(verdict = judged[it.strategyId]?.verdict ?: Verdict.INSUFFICIENT_SAMPLE)
                    }
                )
            )
        }
    }

    private fun resultRows(results: List<PeriodResult>): List<ResultRow> {
        val nameOf = players.associate { it.id to it.displayName }
        val rows = results.map { r ->
            val best = r.settlement.prizes.filter { it.amountYuan > 0 }.maxByOrNull { it.reds }
            val hitText = when {
                r.abstention -> "弃权(0 注)"
                best != null -> "${best.reds}+${if (best.blueHit) 1 else 0}"
                else -> "未中奖"
            }
            ResultRow(
                strategyId = r.strategyId,
                name = nameOf[r.strategyId] ?: r.strategyId,
                ticketsText = ticketSummary(r),
                hitText = hitText,
                winningsYuan = r.settlement.winningsYuan,
            )
        }
        return rows.filter { it.strategyId != "random" } + rows.filter { it.strategyId == "random" } // 对照组最后揭晓(D12)
    }

    private fun ticketSummary(r: PeriodResult): String = when {
        r.abstention -> "—"
        r.tickets.size == 1 -> r.tickets.first().let { it.reds.joinToString(" ") + " +${it.blue}" }
        else -> r.tickets.first().reds.joinToString(" ") + " +${r.tickets.first().blue} …×${r.tickets.size}"
    }

    private fun ticketText(plan: BetPlan): String {
        val first = plan.schemes.first()
        val head = first.reds.joinToString(" ") + " +${first.blue}"
        return if (plan.schemes.size > 1) "$head …×${plan.schemes.size}" else head
    }
}
