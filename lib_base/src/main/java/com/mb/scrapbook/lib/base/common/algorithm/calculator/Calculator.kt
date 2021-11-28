package com.mb.scrapbook.lib.base.common.algorithm.calculator

import java.lang.StringBuilder

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
class Calculator() {

    /**
     * 计算器入口方法
     * 接收一个字符串类型的中缀表达式(exp)，最终返回单精度浮点型计算结果；
     */
    suspend fun calculate(exp: String?): Float = exp?.let {
        /*
         * 1) 使用 exp?.let { ... } ?: 0f 方式提升健壮性，当exp等于Null时，直接返回0.0作为计算结果；
         * 2) 通过it.run标准函数体内逻辑，实现对中缀表达式清洗过程，最终结果存在在expList中缀集合中；
         */
        val expList = mutableListOf<String>().apply {
            var strNum = StringBuilder()
            it.trim().toCharArray().forEach { c ->
                when (c.code) {
                    32 -> { /*ignore: 空格*/ }
                    46, in 48..57 -> strNum.append(c)
                    else -> {
                        add(strNum.toString())
                        add(c.toString())
                        strNum = StringBuilder()
                    }
                }
            }
            if (strNum.isNotEmpty()) {
                add(strNum.toString())
            }
        }
        println(expList)
        0f
    } ?: 0f



}