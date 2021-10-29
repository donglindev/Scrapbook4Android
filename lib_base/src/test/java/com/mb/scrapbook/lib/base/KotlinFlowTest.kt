package com.mb.scrapbook.lib.base

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import java.lang.RuntimeException
import java.util.*
import kotlin.system.measureTimeMillis

/**
 * 入口函数
 */
suspend fun main() {
    // 第一个Flow学习示例
    makeFlow()
}


fun makeFlow() {
    runBlocking {
        // measure time-millis
        val duration = measureTimeMillis {
            flow<Int> {
                for (n in 1 .. 10) {
                    println("UP.TID: ${Thread.currentThread().id}, n: ${n}")
                    emit(n)
                    delay(100)
                }
            }
            .onStart { println("start flow") }
            .flowOn(Dispatchers.IO)
            .buffer()
            .onEach {
                println("onEach.TID: ${Thread.currentThread().id}, value: $it")
                delay(100)
                if (it == 5) {
                    throw RuntimeException()
                }
            }
            .onCompletion { println("completion") }
            .flowOn(Dispatchers.Default)
             .collect {
                println("TID: ${Thread.currentThread().id}, emit.value: ${it}")
             }
        }
        println("flow duration: ${duration}/ms")
    }
}

