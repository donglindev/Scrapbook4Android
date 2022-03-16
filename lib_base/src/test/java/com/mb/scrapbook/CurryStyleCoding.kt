package com.mb.scrapbook

import org.junit.Test

/**
 * 柯理化风格
 * 柯理化风格和击鼓传花游戏类似，由第1个人开始，最后一个人结束并揭晓谜底；
 */
class CurryStyleCoding {

    /**
     * sum是Lambda表达式函数
     * 它也是柯理化风格函数，它由a开始，经过b，最终由c计算结果并返回；
     */
    fun sum(a: Int) = { b: Int ->
        { c: Int -> a + b + c }
    }

    /**
     * 使用柯理化风格按照自定义方式比较两个Array中元素是否相等
     * 难度：***
     */
    fun <A, B> Array<A>.compare (that: Array<B>, block: (A, B) -> Boolean): Boolean {
        val i = this.iterator() // 扩展函数中this表示Array<A>实例的迭代器对象
        val j = that.iterator() // 参数that的迭代器对象
        // 两个迭代器同时遍历
        while (i.hasNext() && j.hasNext()) {
            // 按照自定义方式比较两个Array中元素是否相等
            if (!block(i.next(), j.next())) {
                return false // 不等直接返回
            }
        }
        // 任何一个Array元素多则返回false，否则返回true，并且全部元素比较后相等；
        return (!i.hasNext() && !j.hasNext())
    }

    @Test
    fun main() {
        val instance = CurryStyleCoding()
        // 调用柯理化风格函数
        val n = instance.sum(1)(2)(3)
        println("calculate result: $n") // calculate result: 6

        // 调用两个Array比较函数
        val a = arrayOf(1, 2, 3)
        val b = arrayOf(2, 3 ,4)

        println(a.compare(b) { x, y -> x + 1 == y }) // true
        println(a.compare(b) { x, y -> x + 2 == y }) // false
    }
}