package com.mb.scrapbook.lottery.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.InputStream
import java.io.StringReader

class DrawRepositoryTest {

    private val repo = DrawRepository()

    // ---- 打包历史(classpath 单源, D11=A) ----

    @Test
    fun loadsPackagedHistory() {
        val draws = repo.load()
        assertEquals(3490, draws.size) // T1 入库事实: 03001→26093(刷新历史时同步改此断言)
        assertEquals("03001", draws.first().period)
        assertEquals("26093", draws.last().period)
    }

    @Test
    fun missingResourceFailsLoudly() {
        val broken = DrawRepository(classLoader = object : ClassLoader() {
            override fun getResourceAsStream(name: String): InputStream? = null
        })
        assertThrows(IllegalStateException::class.java) { broken.load() }
    }

    // ---- parse 边界(缺列/越界/乱序, 设计 R10) ----

    private fun parse(vararg lines: String) = repo.parse(StringReader(lines.joinToString("\n")))

    @Test
    fun parsesValidRows() {
        val draws = parse(HEADER, "03001,1,5,11,22,28,33,9,2003-02-23,500.com")
        assertEquals(1, draws.size)
        assertEquals(listOf(1, 5, 11, 22, 28, 33), draws[0].reds)
        assertEquals(9, draws[0].blue)
        assertEquals("2003-02-23", draws[0].date)
    }

    @Test
    fun sortsUnsortedReds() {
        val draws = parse(HEADER, "03001,33,1,11,22,28,5,9,2003-02-23,t")
        assertEquals(listOf(1, 5, 11, 22, 28, 33), draws[0].reds)
    }

    @Test
    fun toleratesUtf8Bom() {
        val draws = parse(0xFEFF.toChar().toString() + HEADER, "03001,1,5,11,22,28,33,9,2003-02-23,x")
        assertEquals(1, draws.size)
    }

    @Test
    fun rejectsBadHeader() {
        assertThrows(IllegalArgumentException::class.java) { parse("period,red1,red2", "03001,1,2") }
    }

    @Test
    fun rejectsEmptyCsv() {
        assertThrows(IllegalArgumentException::class.java) { repo.parse(StringReader("")) }
    }

    @Test
    fun rejectsMissingColumn() {
        assertThrows(IllegalArgumentException::class.java) {
            parse(HEADER, "03001,1,5,11,22,28,9,2003-02-23,x")
        }
    }

    @Test
    fun rejectsNonNumeric() {
        assertThrows(IllegalArgumentException::class.java) {
            parse(HEADER, "03001,1,5,xx,22,28,33,9,2003-02-23,x")
        }
        assertThrows(IllegalArgumentException::class.java) {
            parse(HEADER, "abc,1,5,11,22,28,33,9,2003-02-23,x")
        }
    }

    @Test
    fun rejectsOutOfRangeBall() {
        assertThrows(IllegalArgumentException::class.java) {
            parse(HEADER, "03001,1,5,11,22,28,34,9,2003-02-23,x")
        }
        assertThrows(IllegalArgumentException::class.java) {
            parse(HEADER, "03001,1,5,11,22,28,33,17,2003-02-23,x")
        }
    }

    @Test
    fun rejectsDuplicateRed() {
        assertThrows(IllegalArgumentException::class.java) {
            parse(HEADER, "03001,1,1,11,22,28,33,9,2003-02-23,x")
        }
    }

    @Test
    fun rejectsOutOfOrderPeriod() {
        assertThrows(IllegalArgumentException::class.java) {
            parse(
                HEADER,
                "03001,1,5,11,22,28,33,9,2003-02-23,x",
                "03003,2,5,11,22,28,33,9,2003-02-27,x",
                "03002,3,5,11,22,28,33,9,2003-03-02,x",
            )
        }
    }

    @Test
    fun rejectsDuplicatePeriod() {
        assertThrows(IllegalArgumentException::class.java) {
            parse(
                HEADER,
                "03001,1,5,11,22,28,33,9,2003-02-23,x",
                "03001,2,5,11,22,28,33,9,2003-02-27,x",
            )
        }
    }

    companion object {
        private const val HEADER = "period,red1,red2,red3,red4,red5,red6,blue,date,source"
    }
}
