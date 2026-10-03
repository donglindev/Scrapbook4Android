package com.mb.scrapbook.lottery.core.replay

import com.mb.scrapbook.lottery.core.domain.LedgerEntry
import com.mb.scrapbook.lottery.core.domain.Verdict
import org.junit.Assert.assertEquals
import org.junit.Test

class PValueTest {

    private val nullAsc: DoubleArray = (1..1000).map { it / 1000.0 }.toDoubleArray()

    @Test
    fun twoSidedAtMedianIsOne() {
        assertEquals(1.0, PValue.twoSided(nullAsc, 0.5005), 1e-12)
    }

    @Test
    fun twoSidedBeyondAllIsZeroThenFormatted() {
        assertEquals(0.0, PValue.twoSided(nullAsc, -1.0), 1e-12)
        assertEquals("<0.001", PValue.format(PValue.twoSided(nullAsc, -1.0)))
    }

    @Test
    fun twoSidedAtExtremeIsTwoOverN() {
        assertEquals(0.002, PValue.twoSided(nullAsc, 1.0), 1e-12)   // == max:le=1000, ge=1
        assertEquals(0.002, PValue.twoSided(nullAsc, 0.001), 1e-12) // == min:le=1, ge=1000
    }

    @Test
    fun formatBounds() {
        assertEquals("0.035", PValue.format(0.0349))
        assertEquals("0.001", PValue.format(0.001))
        assertEquals("<0.001", PValue.format(0.0004))
    }

    @Test
    fun judgeBindsAlphaToRosterSizeK() { // α/K 绑定报表阵容(R5)
        val entry = { id: String, roi: Double ->
            LedgerEntry(id, totalStakeYuan = 1000, totalWinningsYuan = (1000 * (1 + roi)).toLong(), hitDistribution = emptyMap())
        }
        val nulls = mapOf(
            // 中位数 0.5005
            "spy" to nullAsc,
            "worst" to nullAsc,
            "better" to nullAsc,
            "mid" to nullAsc,
            "a" to nullAsc,
            "b" to nullAsc,
        )
        // roi=0.0095 → le=9 → p=2×9/1000=0.018:α=0.05 内显著,K=6(阈值 0.0083)外不显著
        // roi=-1.0 低于全部 → p=0 → 任何 K 都显著更差
        // roi=0.999 == max → p=0.002 < 0.0083 → 显著更好(数据警报)
        // roi=0.5 中位 → p=1.0 不显著;roi=0.1/0.2 → p=0.2/0.4 不显著
        val entries6 = listOf(entry("spy", 0.0095), entry("worst", -1.0), entry("mid", 0.5), entry("better", 0.999), entry("a", 0.1), entry("b", 0.2))
        val judged6 = PValue.judge(entries6, nulls)
        assertEquals(Verdict.NOT_SIGNIFICANT, judged6[0].verdict) // p=0.018 > 0.0083
        assertEquals(Verdict.SIGNIFICANTLY_WORSE, judged6[1].verdict) // p=0 < 阈值
        assertEquals(Verdict.NOT_SIGNIFICANT, judged6[2].verdict)
        assertEquals(Verdict.SIGNIFICANTLY_BETTER, judged6[3].verdict) // p=0.002 < 0.0083

        // 同一数据,单选手报表 K=1(阈值 0.05)→ spy 转为显著更差:证明校正绑定阵容
        val judged1 = PValue.judge(listOf(entry("spy", 0.0095)), nulls)
        assertEquals(Verdict.SIGNIFICANTLY_WORSE, judged1[0].verdict)
    }
}
