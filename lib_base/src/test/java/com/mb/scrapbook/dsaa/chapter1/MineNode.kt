package com.mb.scrapbook.dsaa.chapter1

/**
 * 自定义链表节点
 * 此链表是双向链表，支持previous和next域；
 *
 * @author Amit
 * @date 2021/11/10
 */
data class MineNode<T>(var previous: MineNode<T>? = null,
                       val data: T,
                       var next: MineNode<T>? = null)
