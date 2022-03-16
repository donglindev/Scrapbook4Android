package com.mb.scrapbook

import android.speech.tts.TextToSpeech
import android.util.Log
import kotlin.random.Random

/**
 * Kotlin中的单例类
 * 使用object关键字声明的类被翻译成Java代码后类会变成单例模式
 */
object Utils {

    @JvmField var TAG = "Utils"

    /**
     * 此方法是Utils单例类中普通方法
     */
    fun calculatePosition(): Array<Int> {
        return arrayOf(0, 0)
    }
}

/**
 * Log工具类(单例模式)
 */
class LogUtils {
    /**
     * 伴生对象中@JvmStatic表现形式
     * 在伴生对象中被@JvmStatic注解修饰的方法将翻译为public static final形式，但最终会调用伴生对象中方法实现；
     */
    companion object {
        /**
         * @JvmStatic = public static并添加getter和setter方法
         * @val = final 不可变属性
         * 最终翻译成Java代码为
         * public static final String LOG_TAG = "LogUtils"
         */
        @JvmStatic val LOG_TAG = "LogUtils" // public static final String LOG_TAG = "LogUtils"

        /**
         * @JvmStatic = 函数被修饰为 public static
         * 最终翻译成Java代码为
         * public static final void d(String message)
         * 最终会调用 Companion.d() 方法实现
         */
        @JvmStatic fun d(message: String) {
            Log.d(LOG_TAG, message)
        }
    }
}

/**
 * Engineer类使用伴生对象实现静态方法
 */
class Engineer {

    /**
     * @JvmField = public 且不再生成getter和setter方法
     * class Engineer = 普通类中无修饰符
     * val = final 被val修饰的属性不可变
     * 最终翻译成Java代码为
     * public final String uniqueId = "STAFF0xA01"
     */
    @JvmField val uniqueId = "STAFF0xA01" // public final String uniqueId = "STAFF0xA01"

    /**
     * @JvmField = public 且不再生成getter和setter方法
     * class Engineer = 普通类中无修饰符
     * var = 可变属性无修饰符
     * 最终翻译成Java代码为
     * public int fixRecord = 20220306
     */
    @JvmField var fixRecord: Int = 20220306 // public int fixRecord = 20220306

    /**
     * Kotlin中默认伴生对象，使用类名调用伴生对象中属性和方法；
     * 默认伴生对象被翻译成Java代码后，会在类中生成一个名为Companion静态内部类；
     * 外部类通过访问静态内部类实现属性和方法的调用；
     */
    companion object {

        /*
         * @JvmField = public 并不再生成getter和setter方法
         * companion object = static类及修饰符
         * val = final 被val修饰的属性不可变
         * 最终被翻译成Java代码为
         * public static final Random genRandom = new Random(System.currentTimeMillis())
         */
        @JvmField val genRandom = Random(System.currentTimeMillis()) // public static final Random genRandom = new Random(System.currentTimeMillis())

        /**
         * @JvmField = public 不生成getter和setter方法
         * companion object = static类及修饰符
         * var = 可变属性无修饰符
         * 最终被翻译成Java代码为
         * public static boolean toggle = false
         */
        @JvmField var toggle: Boolean = false // public static boolean toggle = false

        /**
         * 使用 Engineer.make() 调用
         */
        fun make(): Engineer = Engineer()
    }
}

/**
 * Machine类使用显示伴生对象实现静态方法
 */
class Machine {

    /**
     * 带有名称的伴生对象
     */
    companion object Param {

        @JvmField val FLAG_MASK = 1 shl 4
        @JvmStatic var VAL_TOGGLE_1 = 1;

        /**
         * 使用 Machine.Param.build() 调用
         */
        fun build(toggle: Boolean): Machine {
            return Machine()
        }
    }
}



suspend fun main() {
    Machine.Param.build(false)
}