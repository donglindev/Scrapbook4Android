package com.mb.scrapbook

import org.junit.Test

class SearchContent {
    /**
     * filterStringContent高阶函数
     * 需求：筛选字符串集合中元素，保留既是数字开头又是叹号结尾的元素
     * ['03/10 日报!', '周报 03/10。', '03/10 月报 !']
     * 输出：['03/10 日报!', '03/10 月报 !']
     *
     * 接收dataList数据集，并接收一个函数类型参数名为condition的条件变量，condition条件变量接收字符串输入并返回条件是否成立结果；
     */
    fun filterStringContent(dataList: MutableList<String>, condition: (String) -> Boolean) {
        /**
         * 仅仅为了代码美观，我们实现dataList的遍历
         */
        val r = mutableListOf<String>()
        for (content in dataList) {
            if (condition(content)) {
                r.add(content)
            }
        }
        println("filterStringContent: $r")
    }

    /**
     * condition是一个普通函数，它接收一个String值返回一个Boolean值；
     * 此函数用于filterStringContent高阶函数中参数类型的使用
     * 注意：任何接收String返回Boolean的函数类型都可以使用此方法，并不一定要叫做condition名字；
     */
    open fun condition(content: String): Boolean {
        return (content.startsWith("0") && content.endsWith("!"))
    }

    @Test
    fun main() {
        val dataList = mutableListOf("03/10 日报!", "周报 03/10。", "03/10 月报 !")
        /**
         * 使用“：：”双冒号法调用高阶函数
         * 传递给高阶函数的参数，方法必须使用对象引用作为前缀
         */
        val instance = SearchContent()
        instance.filterStringContent(dataList, instance::condition)

        /**
         * 使用匿名函数调用高阶函数
         * 匿名函数只是将普通函数三要素中的函数名称去掉，只包含输入参数和输出参数
         * 语法：fun (输入参数): 输出参数 { 函数体 }
         */
        instance.filterStringContent(dataList, fun (content: String): Boolean {
            return (content.startsWith("0") && content.endsWith("!"))
        })

        /**
         * 示例A
         * SearchContent示例中使用匿名函数调用filterStringContent高阶函数，可使用Lambda表达式语法简化；
         */
        instance.filterStringContent(dataList, { // Lambda表达式必须用 { } 包裹
            content: String -> // 输入参数，String类型
            content.startsWith("0") && content.endsWith("!") // 输出参数，Boolean类型
        })

        instance.filterStringContent(dataList) { content ->
            (content.startsWith("0") && content.endsWith("!"))
        }
    }
}