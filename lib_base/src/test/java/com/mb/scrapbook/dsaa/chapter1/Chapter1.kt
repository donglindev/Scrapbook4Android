package com.mb.scrapbook.dsaa.chapter1

import org.junit.Test

/**
 * Data structures and Algorithm Analysis in Java (dsaa)
 * 第一章测试类
 *
 * @author Amit
 * @date 2021/11/09
 */
class Chapter1 {

    /**
     * 习题(2)
     * 1) 实现一个自定义双向链表；
     * 2) 在自定义双向链表中实现contains功能；
     */
    @Test fun exercise2() {

    }

    /**
     * 习题(1)
     * 1) 给定两个表P和L，它们都包含以升序排列的整数；
     * 2) 打印L中由P指定位置上的元素，列如：P=1,3,5 L=7,8,12,17,22,66; R=7,12,22
     */
    @Test fun exercise1() {
        val positionList = listOf(1, 6, 8) // P
        val itemList = listOf(6, 8, 12, 19, 26, 28, 31, 32, 36) // L
        // 通过apply扩展函数，实现习题(1)结果集的创建
        val resultList = mutableListOf<Int>().apply {
            for (position in positionList) {
                val idx = position - 1
                if (idx < 0 || idx >= itemList.size) {
                    continue
                }
                add(itemList.get(idx))
            }
        }
        print("Chapter1.exercise1.result: $resultList")
    }
}