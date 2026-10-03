package com.mb.scrapbook.lottery.strategy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * D24 第一阶梯 fixture 套件:合成语料(真机语料积累后替换)。
 * 阈值 90%:expectParse 组须全过(确定性);实测 <90% 才评估 GBNF 升级。
 */
class LlmTicketParserTest {

    private data class Fixture(val raw: String, val expectParse: Boolean)

    private val clean = """{"reds":[1,5,11,22,28,33],"blue":9,"reason":"频率均衡"}"""

    private val fixtures = listOf(
        // ---- 干净 JSON ----
        Fixture(clean, true),
        Fixture("""{"reds":[2,7,13,19,25,31],"blue":16,"reason":"玄学直觉"}""", true),
        Fixture("""{"reds":[3,8,14,20,26,32],"blue":1,"reason":"遗漏回归"}""", true),
        Fixture("""{"reds":[4,9,15,21,27,33],"blue":2,"reason":"诗句节奏"}""", true),
        Fixture("""{"reds":[5,10,16,22,28,30],"blue":3,"reason":"冷热对冲"}""", true),
        Fixture("""{"reds":[6,11,17,23,29,31],"blue":4}""", true), // reason 缺省容忍
        // ---- markdown 围栏 ----
        Fixture("```json\n$clean\n```", true),
        Fixture("```\n$clean\n```", true),
        Fixture("以下是本注:\n```json\n$clean\n```\n祝好运。", true),
        Fixture("```JSON\n$clean\n```", true),
        // ---- 前后闲聊 ----
        Fixture("好的,统计教授本轮选号如下:\n$clean\n以上,请查收。", true),
        Fixture("嗯…让我感受一下号码的气场…\n$clean", true),
        Fixture("$clean\n(这句诗是:夏夜的第七颗星)", true),
        Fixture("【疯癫诗人】解构与重组:$clean 完。", true),
        // ---- 字段别名 ----
        Fixture("""{"red":[1,5,11,22,28,33],"blue":9,"reason":"r"}""", true),
        Fixture("""{"红球":[1,5,11,22,28,33],"蓝球":9,"理由":"r"}""", true),
        Fixture("""{"reds":[1,5,11,22,28,33],"蓝球":9,"reason":"r"}""", true),
        // ---- 数字为字符串 ----
        Fixture("""{"reds":["1","5","11","22","28","33"],"blue":"9","reason":"r"}""", true),
        Fixture("""{"reds":[1.0,5.0,11.0,22.0,28.0,33.0],"blue":9.0,"reason":"r"}""", true),
        // ---- reason 内嵌花括号/引号(平衡扫描须跳过字符串字面量) ----
        Fixture("""{"reds":[1,5,11,22,28,33],"blue":9,"reason":"小心 {陷阱} \"引号\""}""", true),
        Fixture("""前置说明 {不是票据} 已略。\n$clean""", true),
        Fixture("""{"reds":[1,5,11,22,28,33],"blue":9,"reason":"数据不会说谎,但会开玩笑"}""", true),
        // ---- 截断/畸形 → null(弃权路径) ----
        Fixture("""{"reds":[1,5,11,22,28,33],"blue":9,"reason":"截""", false),
        Fixture("""{"reds":[1,5,11,22,28]""", false),
        // ---- 语义非法 → null ----
        Fixture("""{"reds":[1,1,11,22,28,33],"blue":9,"reason":"重复"}""", false),
        Fixture("""{"reds":[1,5,11,22,28,34],"blue":9,"reason":"越界"}""", false),
        Fixture("""{"reds":[1,5,11,22,28],"blue":9,"reason":"不足6"}""", false),
        Fixture("""{"reds":[1,5,11,22,28,33],"blue":17,"reason":"蓝越界"}""", false),
        Fixture("""{"reds":["甲","乙","丙","丁","戊","己"],"blue":9,"reason":"非数字"}""", false),
        // ---- 纯吐槽非 JSON → null ----
        Fixture("大师我夜观天象,发现号码的秘密不可言说,只能意会。", false),
    )

    @Test
    fun fixtureCorpusMeetsThreshold() {
        val parseable = fixtures.filter { it.expectParse }
        val ok = parseable.count { LlmTicketParser.parse(it.raw) != null }
        assertEquals("expectParse 语料须全过(确定性合成语料)", parseable.size, ok)
        assertTrue("D24 阈值 90%:$ok/${parseable.size}", ok.toDouble() / parseable.size >= 0.9)
    }

    @Test
    fun malformedReturnsNullNotException() {
        fixtures.filter { !it.expectParse }.forEach {
            assertNull("应弃权: ${it.raw.take(30)}…", LlmTicketParser.parse(it.raw))
        }
    }

    @Test
    fun parsedTicketIsNormalized() {
        val t = LlmTicketParser.parse("""{"reds":[33,1,28,11,22,5],"blue":9,"reason":" x "}""")!!
        assertEquals(listOf(1, 5, 11, 22, 28, 33), t.reds) // 排序归一
        assertEquals("x", t.reason) // trim
        assertNotNull(t)
    }
}
