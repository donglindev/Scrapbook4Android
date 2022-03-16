package com.mb.scrapbook

import org.junit.Test

/**
 * Lambda表达式示例
 */
class LambdaExample {

    /**
     * Lambda表达式函数体
     * 语法定义：它是由函数名称、输入参数、输出参数以及等号加大括号方式实现；
     */
    fun display(n: Int) = {
        println("display $n")
    }

    /**
     * 调用Lambda表达式
     */
    @Test
    fun exampleDisplay() {
        // 在forEach中调用Lambda表达式函数display()，只是创建了Function对象，并不会输出任何内容；
        listOf(1, 2, 3).forEach { display(it) }

        // 1) 使用invoke()方法调用Lambda表达式
        listOf(2, 3, 4).forEach { display(it).invoke() }

        // 2) 使用 () 方法调用Lambda表达式
        listOf(7, 8, 9).forEach { display(it)() }

        // 3) 自运行方式
        display(0)();
        { x: Int, y: Int -> println("result: ${x + y}") }(100, 200)
    }
}