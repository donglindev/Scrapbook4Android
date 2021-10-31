package com.mb.scrapbook.lib.base.common.algorithm

import com.mb.scrapbook.lib.base.common.IMatcher
import java.util.*

/**
 * BracketMatcher
 * 括号匹配算法，可支持：
 * 1) 支持“()”，“[]”，“{}”括号；
 * 2) 支持括号嵌套；
 * 3) 支持JSON数据括号检查；
 */
class BracketMatcher(): IMatcher<String> {

    /**
     * 伴生类: 定义常量括号
     */
    private companion object {
        // 小括号
        private const val SMALL_LEFT_BRACKET = '('
        private const val SMALL_RIGHT_BRACKET = ')'
        // 中括号
        private const val MEDIUM_LEFT_BRACKET = '['
        private const val MEDIUM_RIGHT_BRACKET = ']'
        // 大括号
        private const val LARGE_LEFT_BRACKET = '{'
        private const val LARGE_RIGHT_BRACKET = '}'
        // 括号映射关系，左 -> 右，右 -> 左
        private val BRACKET_MAPPING = mutableMapOf<Char, Char>(
            SMALL_LEFT_BRACKET    to SMALL_RIGHT_BRACKET,
            SMALL_RIGHT_BRACKET   to SMALL_LEFT_BRACKET,
            MEDIUM_LEFT_BRACKET   to MEDIUM_RIGHT_BRACKET,
            MEDIUM_RIGHT_BRACKET  to MEDIUM_LEFT_BRACKET,
            LARGE_LEFT_BRACKET    to LARGE_RIGHT_BRACKET,
            LARGE_RIGHT_BRACKET   to LARGE_LEFT_BRACKET
        )
    }

    /**
     * 匹配括号算法
     * 使用栈数据结构实现括号匹配，算法如下：
     *     1) 将文本数据按字符分割成数组；
     *     2) 逐位判断字符是否是括号；
     *   3.1) 遇到左括号时入栈；
     * 3.2.1) 遇到右括号时，根据右括号在mapping中取到对应的左括号；
     * 3.2.2) 并将元素出栈，将出栈元素对应的左括号比较；
     * 3.2.3) 相等：继续循环比较；不等：直接反馈匹配失败；
     *   3.4) 循环结束，若栈中依然遗留括号，说明左括号多了，匹配失败；
     *   3.5) 上述情况均不符合时，表示括号匹配成功；
     */
    override fun match(data: String?): Boolean {
        return data?.let {
            val stack = Stack<Char>() // ADT
            val array = it.toCharArray() // 1)
            for (character in array) {
                when (character) {
                    // 2)
                    SMALL_LEFT_BRACKET, MEDIUM_LEFT_BRACKET, LARGE_LEFT_BRACKET -> {
                        stack.push(character)
                    }
                    // 3.2.1), 3.2.2), 3.2.3)
                    SMALL_RIGHT_BRACKET, MEDIUM_RIGHT_BRACKET, LARGE_RIGHT_BRACKET -> {
                        // stack.isEmpty解决"()]"右括号多的情况
                        if (stack.isEmpty()) {
                            return false
                        } else if (!stack.pop().equals(BRACKET_MAPPING[character])) {
                            return false
                        }
                    }
                }
            }
            // 3.4)
            if (!stack.isEmpty()) {
                return false
            }
            // 3.5
            return true
        } ?: false
    }
}
