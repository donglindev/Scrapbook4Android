package com.mb.scrapbook.lib.base.common.algorithm.calculator

import java.lang.StringBuilder
import java.util.*
import java.util.regex.Pattern

/**
 * Calculator for Kotlin
 * 使用Kotlin描述后缀表达式（逆荷兰）方法，实现简单计算器表达式解析逻辑；
 *
 * 中缀表达式:
 * 1) a + b * c
 * 2) (a + b) * (c + d)
 *
 * 后缀表达式
 * 1) "abc*+"表示，bc的积加上a的和计算出的结果就是中缀表达式 a + b * c 的结果；
 * 2) "ab+cd+*"表示，ab的和与cd的和的积计算的结果就是中缀表达式 (a + b) * (c + d)的结果；
 *
 * 中缀表达式转换成后缀表达式，要遵守以下规则：
 *  1) 数字和符号需要指定级别，数字<括号<加法/减法<乘法/除法<幂运算；
 *  2) 使用栈数据结构处理操作符级别，它表示操作符栈；
 * 2a) 当操作符栈为空时，新操作符直接入栈；
 * 2b) 当新操作符是左括号时，左括号操作符直接入栈；
 * 2c) 当新操作符级别大于栈顶操作符级别时，新操作符直接入栈；
 * 2d) 当新操作符级别小于等于栈顶操作符时，栈顶操作符出栈并添加到后缀表达式队列，最后将新操作符入栈；
 * 2e) 当新操作符是右括号时，操作符栈元素出栈直至左括号位置，并将出栈的操作符添加到后缀表达式队列；
 *
 * 后缀表达式队列
 * 1) 按顺序存放上述中缀转后缀表达式规则计算出的结果；
 * 2) 建议使用链表数据结构实现后缀表达式队列（删除操作时间复杂度O(1)）；
 *
 * @author Amit
 * @date 2021/11/07
 */
object Calculator {

    /** 操作符号密封类 */
    sealed class Symbol(val symbol: String, val priority: Int) {
        /** 空 */
        class Empty: Symbol("", -1)
        /** 数字 */
        class Number(num: String): Symbol(num, 0)
        /** 左括号 */
        class LeftBracket: Symbol("(", 1)
        /** 右括号 */
        class RightBracket: Symbol(")", 1)
        /** 加法 */
        class Plus: Symbol("+", 2)
        /** 减法 */
        class Minus: Symbol("-", 2)
        /** 乘法 */
        class Times: Symbol("*", 3)
        /** 除法 */
        class Div: Symbol("/", 3)

        override fun toString(): String {
            return symbol
        }
    }

    /**
     * 根据符号创建密封类对象
     */
    private fun makeSymbol(symbol: String): Symbol = when (symbol) {
        "(" -> Symbol.LeftBracket()
        ")" -> Symbol.RightBracket()
        "+" -> Symbol.Plus()
        "-" -> Symbol.Minus()
        "*" -> Symbol.Times()
        "/" -> Symbol.Div()
        else -> Symbol.Empty()
    }

    /**
     * 计算器入口方法
     * 接收一个字符串类型的中缀表达式(exp)，最终返回单精度浮点型计算结果；
     * 使用 exp?.let { ... } ?: 0f 方式提升健壮性，当exp等于Null时，直接返回0.0作为计算结果；
     */
    suspend fun calculate(exp: String?): Float = exp?.let {
        // 1) 创建后缀表达式集合，此表达式集合用于计算结果
        val expSuffixList = makeSuffixExp(exp)
        println("suffix exp: $expSuffixList")

        var result = 0f;
        while (expSuffixList.size > 1) {
            expSuffixList.forEachIndexed { index, symbol ->
                if (symbol !is Symbol.Empty && symbol !is Symbol.Number) {
                    val idxN1 = index - 2
                    val idxN2 = index - 1
                    val expN1 = expSuffixList[idxN1]
                    val expN2 = expSuffixList[idxN2]
                    if (expN1 is Symbol.Number && expN2 is Symbol.Number) {
                        val valN1 = expN1.symbol.toFloat()
                        val valN2 = expN2.symbol.toFloat()
                        when (symbol) {
                            is Symbol.Plus ->   { result = valN1 + valN2 }
                            is Symbol.Minus ->  { result = valN1 - valN2 }
                            is Symbol.Times ->  { result = valN1 * valN2 }
                            is Symbol.Div ->    { result = valN1 / valN2 }
                            else -> { result = 0f }
                        }
                    }
                    expSuffixList.removeAt(index)
                    expSuffixList.removeAt(idxN2)
                    expSuffixList.removeAt(idxN1)
                    expSuffixList.add(idxN1, Symbol.Number(result.toString()))
                }
            }
        }
        expSuffixList[0].symbol.toFloat()
    } ?: 0f

    /**
     * 根据参数exp中缀表达式文本，创建后缀表达式集合；
     */
    private fun makeSuffixExp(exp: String): LinkedList<Symbol> =
        LinkedList<Symbol>().apply {
            val regexNum = "\\d+" // 数字正则表达式字符串
            var itemInStack: Symbol
            val stackSymbol = Stack<Symbol>() // 符号栈

            // 对参数exp表达式进行清洗，生成中缀表达式集合，集合中的每一个元素表示数字或符号；
            makeInfixExp(exp).forEach { item ->
                when {
                    // 1) 数字；
                    Pattern.matches(regexNum, item.symbol) -> { add(item) }
                    // 2a) 当操作符栈为空时，新操作符直接入栈；
                    stackSymbol.isEmpty() -> { stackSymbol.push(item) }
                    // 2b) 当新操作符是左括号时，左括号操作符直接入栈；
                    (item is Symbol.LeftBracket) -> { stackSymbol.push(item) }
                    // 当新操作符是右括号时，操作符栈元素出栈直至左括号位置，并将出栈的操作符添加到后缀表达式队列；
                    (item is Symbol.RightBracket) -> {
                        while (stackSymbol.isNotEmpty()) {
                            itemInStack = stackSymbol.pop()
                            if (itemInStack is Symbol.LeftBracket) {
                                break
                            }
                            add(itemInStack)
                        }
                    }
                    (item is Symbol) -> {
                        itemInStack = stackSymbol.peek();
                        val diff = (item.priority - itemInStack.priority)
                        // 2c) 当新操作符级别大于栈顶操作符级别时，新操作符直接入栈；
                        if (diff > 0) {
                            stackSymbol.push(item)
                        }
                        // 2d) 当新操作符级别小于等于栈顶操作符时，栈顶操作符出栈并添加到后缀表达式队列，最后将新操作符入栈；
                        else if (diff <= 0) {
                            add(stackSymbol.pop())
                            stackSymbol.push(item)
                        }
                    }
                }
            }
            while (stackSymbol.isNotEmpty()) {
                add(stackSymbol.pop())
            }

        }

    /**
     * 对参数exp表达式进行清洗，生成中缀表达式集合，集合中的每一个元素表示数字或符号；
     *
     * 表达式清洗规则：
     * 1) 去掉所有空格；character code=32=space
     * 2) 支持多位数字和浮点数；123,9999992,3.14
     * 3) 支持标准二元运算符和括号；
     */
    private fun makeInfixExp(exp: String): MutableList<Symbol> =
        mutableListOf<Symbol>().apply {
            var num = StringBuilder()
            exp.trim().toCharArray().forEach { c ->
                when (c.code) {
                    32 -> { /* 1) ignore */ }
                    46, in 48..57 -> num.append(c) // 2)
                    else -> { // 3)
                        println("symbol: ${c.toString()}, num: $num")
                        if (num.isNotEmpty()) {
                            add(Symbol.Number(num.toString()))
                        }
                        add(makeSymbol(c.toString()))
                        num = StringBuilder()
                    }
                }
            }
            if (num.isNotEmpty()) {
                add(Symbol.Number(num.toString()))
            }
        }
}