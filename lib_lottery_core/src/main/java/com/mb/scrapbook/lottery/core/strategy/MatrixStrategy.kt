package com.mb.scrapbook.lottery.core.strategy

import com.mb.scrapbook.lottery.core.domain.BetPlan
import com.mb.scrapbook.lottery.core.domain.BetScheme
import com.mb.scrapbook.lottery.core.domain.Draw
import com.mb.scrapbook.lottery.core.domain.NumberPool

/**
 * 旋转矩阵(设计 R4,lotterylab matrix.py 移植,贪心逻辑不变):
 * 频率派产出 NumberPool(默认 12 红) → 「中P保T」前提组合全覆盖贪心缩水 → 蓝球全配对。
 * 位掩码实现(Long,n ≤ 33);n==6 特判短路。
 * 诚实边界:改变的是前提成立时的命中结构(方差形状),不改变期望值——UI 与 README 必须同句声明。
 */
class MatrixStrategy(
    private val p: Int = DEFAULT_P,
    private val t: Int = DEFAULT_T,
) : Strategy {

    private val pooler = FrequencyStrategy()

    override val id = "matrix"
    override val displayName = "旋转矩阵"
    override val requiresModel = false

    override fun pick(ctx: PickContext): BetPlan = shrink(pooler.pool(ctx))

    /** pool → BetPlan:全前提覆盖是构造目标,贪心后仍有剩余即 fail loudly(不应发生)。 */
    fun shrink(pool: NumberPool): BetPlan {
        val numbers = pool.redPool.sorted()
        val blues = pool.bluePool.sorted()
        val n = numbers.size

        // 特判短路(matrix.py:0):n=6 时 n<p 的参数校验会误伤;唯一 6 红 × 蓝球全配对。
        if (n == Draw.RED_COUNT) {
            return plan(n, blues.map { BetScheme(numbers, it) }, shortcut = true)
        }
        require(n >= p && p >= t && t >= 1 && n >= Draw.RED_COUNT) {
            "参数不合法: 号码数($n) >= 中奖前提($p) >= 保底($t), 且号码数 >= ${Draw.RED_COUNT}"
        }

        val targets = combinations(n, p)
        val targetMasks = LongArray(targets.size) { i -> toMask(targets[i]) }
        val candidates = combinations(n, Draw.RED_COUNT)
        val words = (targets.size + Long.SIZE_BITS - 1) ushr 6

        // 覆盖矩阵:candidate 位向量,bit i = 该候选注对前提 i 满足 |交| ≥ t
        val coverage = Array(candidates.size) { LongArray(words) }
        for (ci in candidates.indices) {
            val candidateMask = toMask(candidates[ci])
            for (ti in targets.indices) {
                if (java.lang.Long.bitCount(candidateMask and targetMasks[ti]) >= t) {
                    coverage[ci][ti ushr 6] = coverage[ci][ti ushr 6] or (1L shl (ti and 63))
                }
            }
        }

        val remaining = LongArray(words) { -1L }
        if (targets.size and 63 != 0) {
            remaining[words - 1] = (1L shl (targets.size and 63)) - 1 // 清 padding 位
        }
        val selected = ArrayList<IntArray>()
        while (remaining.any { it != 0L }) {
            var best = -1
            var bestGain = 0
            for (ci in coverage.indices) {
                var gain = 0
                for (w in 0 until words) gain += java.lang.Long.bitCount(coverage[ci][w] and remaining[w])
                if (gain > bestGain) {
                    bestGain = gain
                    best = ci
                }
            }
            if (best < 0) break // 无候选可推进(数学上不应发生,见下 check)
            selected += candidates[best]
            for (w in 0 until words) remaining[w] = remaining[w] and coverage[best][w].inv()
        }
        check(remaining.all { it == 0L }) { "矩阵贪心未覆盖全部前提(p=$p, t=$t, n=$n) —— 不应发生,请检查参数" }

        // 索引还原号码(combinations 生成即升序,reds 天然升序);蓝球全配对
        val schemes = selected.flatMap { combo ->
            val reds = combo.map { numbers[it] }
            blues.map { BetScheme(reds, it) }
        }
        return plan(n, schemes, shortcut = false)
    }

    private fun plan(n: Int, schemes: List<BetScheme>, shortcut: Boolean): BetPlan = BetPlan(
        schemes = schemes,
        generatedAt = 0L, // 回放确定性,真实时间戳由 arena 层(T5)落 PeriodResult
        config = mapOf(
            "strategy" to id,
            "p" to p.toString(),
            "t" to t.toString(),
            "n" to n.toString(),
            "shortcut" to shortcut.toString(),
        ),
    )

    private fun toMask(indices: IntArray): Long = indices.fold(0L) { m, i -> m or (1L shl i) }

    companion object {
        /** 默认预设(D20-B 拍板 2026-10-03):12 红 + 1 蓝,中6保5 → 46 注 = ¥92/期(贪心确定性)。
         *  原 ≈25 注/¥50 目标经 P∈{6..8}×T∈{3..5} 网格校准证实数学上不可达,详见设计 R4/D20。 */
        const val DEFAULT_P = 6
        const val DEFAULT_T = 5
        const val POOL_RED_COUNT = 12
    }
}

/** C(n,r) 全组合,字典序(等价 itertools.combinations);元素为 0..n-1 索引,升序。 */
internal fun combinations(n: Int, r: Int): List<IntArray> {
    require(r in 0..n) { "r 必须在 0..n: r=$r, n=$n" }
    val out = ArrayList<IntArray>()
    val cur = IntArray(r)
    fun rec(start: Int, depth: Int) {
        if (depth == r) {
            out += cur.copyOf()
            return
        }
        for (i in start..n - (r - depth)) {
            cur[depth] = i
            rec(i + 1, depth + 1)
        }
    }
    rec(0, 0)
    return out
}
