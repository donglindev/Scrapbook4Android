package com.mb.scrapbook.lib.base

import com.mb.scrapbook.lib.base.common.algorithm.calculator.Calculator

suspend fun main(args: Array<String>?) {
    val exp = "2*(2+3)"
    val result = Calculator.calculate(exp)
    println("calculate.result: $result")
}