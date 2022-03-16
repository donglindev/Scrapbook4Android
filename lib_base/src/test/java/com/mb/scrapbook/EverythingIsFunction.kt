package com.mb.scrapbook

import org.junit.Test

class EverythingIsFunction {

    @Test
    fun main() {
        val obj = EverythingIsFunction()
        val r = obj.expSumInt(10, 10)
        println("10+10=$r")
    }

    /**
     * 计算两个整型值求和
     */
    fun sumInt(a: Int, b: Int): Int {
        return (a + b)
    }

    /**
     * 计算两个整型值求和函数表达式
     */
    fun expSumInt(a: Int, b: Int): Int = (a + b)

    /**
     * Lambda表达式示例
     */
    @Test
    fun lambdaExample() {
        /**
         * 示例B
         * 定义求和Lambda变量sum，并将计算x+y的结果返回；
         * sum：表示Lambda表达式变量，变量的类型由等号右侧的Lambda表达式定义；
         * { x: Int, y: Int -> x + y }：表示Lambda表达式；
         */
        val sum = { x: Int, y: Int -> x + y }

        /**
         * 示例C
         * 定义减法Lambda变量minus，并将计算x-y的结果返回；
         * minus: 表示Lambda表达式变量；
         * (Int, Int) -> Int：表示minus变量的函数类型；
         * { x, y -> x - y }：表示Lambda表达式；
         */
        val minus: (Int, Int) -> Int = { x, y -> x - y }

        /**
         * 示例D
         * 定义除法Lambda变量div，并将计算x/y的结果返回；
         * div: 表示Lambda表达式变量；
         * { x: Float, y: Float -> 计算 }: 表示Lambda表达式
         */
        val div = { x: Float, y: Float ->
            var v = x;
            if (v <= 0) {
                v = 100f
            }
            (v / y) // 最后一行是返回值及类型
        }

        /**
         * 示例E
         * 定义乘法Lambda变量multiplication，并将计算x扩大固定2倍的结果返回；
         * multiplication: 表示Lambda表达式变量；
         * (Int, Int) -> Int: 表示multiplication变量函数类型；
         * { x, _ -> x * 2 }: 表示Lambda表达式；
         * 表达式中“_”表示占位符，不接收第二个Int类型参数；
         */
        val multiplication: (Int, Int) -> Int = { x, _ -> x * 2 }
    }

}