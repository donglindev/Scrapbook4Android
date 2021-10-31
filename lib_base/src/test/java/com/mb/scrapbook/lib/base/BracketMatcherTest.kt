package com.mb.scrapbook.lib.base

import com.mb.scrapbook.lib.base.common.algorithm.BracketMatcher

class BracketMatcherTest() {

    fun testBracketMatcher(data: String) {
        val matcher = BracketMatcher()
        println("'data content' is matched? ${matcher.match(data)}")
    }
}

suspend fun main() {
    val s1 = "a*(b+c)" // true
    val s2 = "a-[(b+c)/d}" // false
    val s3 = "({[[[]]]}))" // false

    var test = BracketMatcherTest()
    test.testBracketMatcher(s1)
    test.testBracketMatcher(s2)
    test.testBracketMatcher(s3)
}