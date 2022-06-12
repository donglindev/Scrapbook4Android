package com.mobean.lib.desginpattern

import org.junit.Before
import org.junit.Test

/**
 * Prototype Pattern
 *
 * 原型设计模式
 * 通过原型模式创建不同显示器对象，并通过管理器使用；
 */

/**
 * 显示器接口
 * 定义克隆方法及显示方法
 *
 * KT bug: https://youtrack.jetbrains.com/issue/KT-24193
 * 如果将Display变成interface形式定义，并且实现Cloneable接口，最终调用clone方法时会出现异常；
 */
abstract class Display : Cloneable {
    /**
     * 克隆方法
     */
    abstract fun createClone() : Display

    /**
     * 显示方法
     */
    abstract fun display(content: String)
}

/**
 * 消息盒子显示器
 */
class MessageBox(private val symbol: Char) : Display() {
    /**
     * 实现克隆方法
     */
    override fun createClone(): Display = try {
        (super.clone() as? MessageBox)?.run { this } ?: MessageBox(symbol)
    } catch (exception: CloneNotSupportedException) {
        MessageBox(symbol)
    }

    /**
     * 实现显示方法
     */
    override fun display(content: String) = println("'$symbol' MessageBox instance.")
}

/**
 * 下划线显示器
 */
class UnderlineBox : Display() {
    /**
     * 实现克隆方法
     */
    override fun createClone(): Display = try {
        (super.clone() as? UnderlineBox)?.run { this } ?: UnderlineBox()
    } catch (exception: CloneNotSupportedException) {
        UnderlineBox()
    }

    /**
     * 实现显示方法
     */
    override fun display(content: String) = println("UnderlineBox instance.")
}

/**
 * 显示器管理类
 */
class DisplayManager(
    /** 显示器注册表 */
    private val displayRegistry: MutableMap<String, Display> = mutableMapOf()
) {
    /**
     * 注册显示器
     */
    fun register(uniqueId: String, display: Display) = displayRegistry.put(uniqueId, display)

    /**
     * 使用原型创建新实例
     */
    fun create(uniqueId: String) : Display {
        val prototype = displayRegistry.get(uniqueId)
        return prototype?.createClone() ?: UnderlineBox()
    }
}

/**
 * 入口方法
 */
class PrototypePattern {
    // 显示器管理类
    private val mgrDisplay: DisplayManager = DisplayManager()

    companion object {
        const val KEY_UNDER_LINE = "UNDERLINE"
        const val KEY_BOX_A = "BOX_A"
        const val KEY_BOX_B = "BOX_B"
    }
    /**
     * 注册显示到管理器中
     */
    @Before fun onInit() {
        val underline = UnderlineBox()
        val boxA = MessageBox('A')
        val boxB = MessageBox('B')
        mgrDisplay.register(KEY_UNDER_LINE, underline)
        mgrDisplay.register(KEY_BOX_A, boxA)
        mgrDisplay.register(KEY_BOX_B, boxB)
    }

    /**
     * 入口方法
     */
    @Test fun demoPrototypePattern() {
        /*
         * 0 -- com.mobean.lib.desginpattern.UnderlineBox@5bcea91b
         * 0 +++ com.mobean.lib.desginpattern.MessageBox@5f3a4b84
         * 1 -- com.mobean.lib.desginpattern.UnderlineBox@27f723
         * 1 === com.mobean.lib.desginpattern.MessageBox@670b40af
         * 2 -- com.mobean.lib.desginpattern.UnderlineBox@4923ab24
         * 2 +++ com.mobean.lib.desginpattern.MessageBox@44c8afef
         * 3 -- com.mobean.lib.desginpattern.UnderlineBox@7b69c6ba
         * 3 === com.mobean.lib.desginpattern.MessageBox@46daef40
         * 4 -- com.mobean.lib.desginpattern.UnderlineBox@12f41634
         * 4 +++ com.mobean.lib.desginpattern.MessageBox@13c27452
         */
        repeat(5) {
            val under = mgrDisplay.create(KEY_UNDER_LINE)
            println("$it -- $under")
            if (it % 2 == 0) {
                println("$it +++ ${mgrDisplay.create(KEY_BOX_A)}")
            } else {
                println("$it === ${mgrDisplay.create(KEY_BOX_B)}")
            }
        }
    }
}