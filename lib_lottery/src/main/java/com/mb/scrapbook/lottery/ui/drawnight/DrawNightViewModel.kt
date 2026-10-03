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
import com.mb.scrapbook.lottery.data.DrawEntryValidator
import com.mb.scrapbook.lottery.data.SeasonStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * 开奖夜状态机(D6=A 三态:出票→锁定→结算)。T6 为 JVM 阵容 + 程序化轮演;
 * LLM persona 流式与相机种子在 T7/T8 接入。
 */
class DrawNightViewModel(app: Application) : AndroidViewModel(app) {

    private val store = SeasonStore(app, viewModelScope)
    private val roster: List<Strategy> = listOf(
        RandomStrategy(), FrequencyStrategy(), ColdHotStrategy(),
        MissingStrategy(), MatrixStrategy(), PixelSeedStrategy(),
    )
    private val nameOf = roster.associate { it.id to it.displayName }

    sealed class Ui {
        data class Idle(val nextPeriod: String, val settledCount: Int) : Ui()
        data class Picking(val index: Int, val total: Int, val name: String, val doneTickets: List<Pair<String, String>>) : Ui()
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

    private val _ui = MutableLiveData<Ui>(Ui.Idle("…", 0))
    val ui: LiveData<Ui> = _ui

    private var runner: SeasonRunner? = null
    private var currentPeriod: String = ""

    init {
        viewModelScope.launch {
            store.load { DrawRepository().load() }
            _ui.value = Ui.Idle(store.nextPeriod(), store.settledDraws.size)
        }
    }

    /** 出票态:聚光灯逐位轮演(D10=A),600ms/位。 */
    fun startPeriod() {
        if (_ui.value is Ui.Picking) return
        viewModelScope.launch {
            currentPeriod = store.nextPeriod()
            val runner = SeasonRunner(roster.map { it.id }.toSet())
            val seedBase = "$currentPeriod|S1".hashCode().toLong()
            val done = mutableListOf<Pair<String, String>>()
            roster.forEachIndexed { i, s ->
                _ui.value = Ui.Picking(i + 1, roster.size, s.displayName, done.toList())
                delay(600)
                val plan = s.pick(PickContext(currentPeriod, store.history(), Random(seedBase * 31L + i)))
                runner.recordPick(s.id, currentPeriod, plan, pickAt = System.currentTimeMillis())
                done += s.displayName to ticketText(plan)
            }
            this@DrawNightViewModel.runner = runner
            _ui.value = Ui.Locked(currentPeriod, done)
        }
    }

    /** D14:录入确认后结算。返回错误文案或 null(成功)。 */
    fun confirmEntry(reds: List<Int>, blue: Int): String? {
        DrawEntryValidator.validate(reds, blue)?.let { return it }
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
        _ui.value = Ui.Idle(store.nextPeriod(), store.settledDraws.size)
    }

    /** live 判决刷新(< 30 期全部「样本不足」,≥ 30 期 horizon null 判决)。 */
    private fun refreshVerdicts() {
        viewModelScope.launch(Dispatchers.Default) {
            val current = _ui.value as? Ui.Resulted ?: return@launch
            val judged = LedgerAggregator.judgeLive(store.archive, store.settledDraws)
            _ui.postValue(
                current.copy(rows = current.rows.map { it.copy(verdict = judged[it.strategyId]?.verdict ?: Verdict.INSUFFICIENT_SAMPLE) })
            )
        }
    }

    private fun resultRows(results: List<PeriodResult>): List<ResultRow> {
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
        return (rows.filter { it.strategyId != "random" } + rows.filter { it.strategyId == "random" }) // 对照组最后揭晓(D12)
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
