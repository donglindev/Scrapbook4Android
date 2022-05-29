package com.mobean.lib.desginpattern

import org.junit.Test
import java.lang.StringBuilder
import java.nio.charset.Charset

/**
 * Template Method Design Pattern
 * 模板方法设计模式
 *
 * 定义一个算法实现对内容循环输出：
 * 1）当输出内容是字符时，字符前后要追加<<content>>;
 * 2）当输出内容是字符串时，字符串前后要追加+-----+content+-----+
 */

/**
 * 模板方法模式
 * 定义算法框架并定义模板方法
 */
abstract class AbstractDisplay<T>(protected val content: T,
                                  private val displayCount: Int = 1) {
    /**
     * 模板方法
     * 用于定义算法框架
     */
    fun display() {
        showStart() // 显示内容前输出前缀
        repeat(displayCount) {
            showContent()
        }
        showEnd() // 显示内容后输出后缀
    }

    /**
     * 子类实现输出内容前显示的内容
     */
    protected abstract fun showStart(): Unit

    /**
     * 子类实现输出内容后显示的内容
     */
    protected abstract fun showEnd(): Unit

    /**
     * 子类实现内容显示
     */
    protected abstract fun showContent(): Unit
}

/**
 * 字符子类，实现打印<<<ccc>>>
 */
class CharDisplay(c: Char): AbstractDisplay<Char>(c, 3) {
    /**
     * 实现算法第一步
     */
    override fun showStart() = print("<<<")

    /**
     * 实现算法第三步
     */
    override fun showEnd() = println(">>>")

    /**
     * 实现算法第二步
     */
    override fun showContent() = print("$content")
}

/**
 * 字符串子类，实现打印
 * +---------+
 * + content +
 * + content +
 * +---------+
 */
class StringDisplay : AbstractDisplay<String> {
    /**
     * 内容宽度
     */
    private val contentWidth: Int by lazy { content.toByteArray().size }

    /**
     * +2构造器
     */
    constructor(content: String, displayCount: Int = 5) : super(content, displayCount)

    /**
     * 实现算法第一步
     */
    override fun showStart() = printLine()

    /**
     * 实现算法第三步
     */
    override fun showEnd() = printLine()

    /**
     * 实现算法第二步
     */
    override fun showContent() {
        println(
            with(StringBuilder(), {
                append("+").append(content).append("+").toString()
            })
        )
    }

    /**
     * 打印前缀和后缀
     */
    private fun printLine() {
        StringBuilder().apply {
            append("+")
            repeat(contentWidth) {
                append("-")
            }
            append("+")
            println(toString())
        }
    }
}

/**
 * 模板方法模式入口类
 */
class TemplateMethodDesignPattern {
    /**
     * 实现模板方法模式的字符串输出入口方法
     */
    @Test fun demoDisplayString() {
        val displayString = StringDisplay(" Hi, Lin ")
        displayString.display()
    }

    /**
     * 实现模板方法模式的字符输出入口方法
     */
    @Test fun demoDisplayChar() {
        val displayChar = CharDisplay('D')
        displayChar.display()
    }
}