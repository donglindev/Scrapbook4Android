package com.mb.scrapbook.lottery.ui.table

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mb.scrapbook.lottery.core.data.DrawRepository
import com.mb.scrapbook.lottery.core.replay.NullDistribution
import com.mb.scrapbook.lottery.core.replay.PValue
import com.mb.scrapbook.lottery.core.replay.WalkForwardReplay
import com.mb.scrapbook.lottery.core.strategy.ColdHotStrategy
import com.mb.scrapbook.lottery.core.strategy.FrequencyStrategy
import com.mb.scrapbook.lottery.core.strategy.MatrixStrategy
import com.mb.scrapbook.lottery.core.strategy.MissingStrategy
import com.mb.scrapbook.lottery.core.strategy.PixelSeedStrategy
import com.mb.scrapbook.lottery.core.strategy.RandomStrategy
import com.mb.scrapbook.lottery.core.strategy.Strategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * JVM 走前回放判决表(里程碑 A 同款,seed=2026L 同种子可复现)。
 * 活动作用域共享:竞技场与诚实账本同表。首次计算分钟级 → 骨架屏(R11 状态矩阵)。
 */
class TableViewModel : ViewModel() {

    val roster: List<Strategy> = listOf(
        RandomStrategy(), FrequencyStrategy(), ColdHotStrategy(),
        MissingStrategy(), MatrixStrategy(), PixelSeedStrategy(),
    )

    private val _rows = MutableLiveData<List<JudgedRow>?>()
    val rows: LiveData<List<JudgedRow>?> = _rows

    init {
        viewModelScope.launch(Dispatchers.Default) {
            val history = DrawRepository().load()
            val result = WalkForwardReplay(roster, seed = WalkForwardReplay.DEFAULT_SEED).run(history)
            val nulls = NullDistribution()
            val shared1 = nulls.roi(history, 1)
            val matrixNull = nulls.roi(history, 46)
            val nullMap = roster.associate { s -> s.id to if (s.id == "matrix") matrixNull else shared1 }
            val judged = PValue.judge(result.ledgers.values.toList(), nullMap)
            val nameOf = roster.associate { it.id to it.displayName }
            _rows.postValue(
                judged.map { JudgedRow.of(it, nameOf.getValue(it.strategyId)) }
                    .sortedByDescending { it.roi }
                    .let { list -> list.filter { it.strategyId != "random" } + list.filter { it.strategyId == "random" } } // 对照组固定末位(D4=A)
            )
        }
    }
}
