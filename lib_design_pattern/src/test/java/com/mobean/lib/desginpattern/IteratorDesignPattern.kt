package com.mobean.lib.desginpattern

import org.junit.Test

/**
 * Iterator Design Pattern
 *
 * 迭代器模式
 * 1) 实现标准迭代器模式；
 * 2) 实现一种持有聚合对象的迭代器模式；
 */

/**
 * 迭代器接口
 * 它描述聚合对象的遍历方法
 */
interface IIterator<T> {

    /**
     * 聚合对象中当前元素是否有效
     */
    fun hasNext(): Boolean

    /**
     * 获得聚合对象中当前元素对象
     */
    fun next(): T
}

/**
 * 聚合对象接口
 * 它描述聚合对象创建迭代器对象的方法
 */
interface IAggregate<T> {

    /**
     * 创建迭代器对象
     */
    fun iterator(): IIterator<T>
}

/**
 * 标准迭代器模式实现类
 * AggregateImpl类实现IAggregate接口，并通过内部类形式定义迭代器对象；
 */
class AggregateImpl() : IAggregate<Char> {

    /**
     * 聚合对象中持有的数据集对象
     */
    private val mDataList: MutableList<Char> = mutableListOf()

    init {
        /**
         * 默认构造器中初始化聚合对象数据
         */
        mDataList.addAll("ABCDEFGHIJKLMNOPQRSTUVWXYZ".toList())
    }

    /**
     * 创建内部类形式的迭代器对象
     */
    override fun iterator(): IIterator<Char> = DefaultIterator()

    /**
     * 内部类形式定义迭代器实现类
     */
    private inner class DefaultIterator : IIterator<Char> {

        /**
         * 迭代过程中index位
         */
        private var mIndex: Int = 0

        /**
         * 判断mDataList对象中mIndex位元素是否有效
         */
        override fun hasNext(): Boolean = (mIndex < mDataList.size)

        /**
         * 获得mDataList对象中mIndex位元素对象
         */
        override fun next(): Char = mDataList[mIndex++]
    }
}

/**
 * 迭代器模式入口类
 */
class IteratorDesignPattern {

    /**
     * 标准迭代器模式入口方法
     */
    @Test fun demoIteratorPattern() {
        val impl = AggregateImpl()
        val it = impl.iterator()
        while (it.hasNext()) {
            println(it.next())
        }
    }

    /**
     * 委托迭代器模式入口方法
     */
    @Test fun demoIteratorPattern2() {
        val impl = AggregateImpl2()
        val it = impl.iterator()
        while (it.hasNext()) {
            println(it.next())
        }
    }
}

/**
 * 一种持有聚合对象的迭代器模式实现类
 * AggregateImpl2类实现IAggregate接口，并通过委托方式将聚合对象委托给迭代器对象；
 */
class AggregateImpl2 : IAggregate<Int> {

    /**
     * 聚合对象中持有的数据集对象
     */
    private val mDataList = mutableListOf<Int>()

    init {
        /**
         * 默认构造器中初始化聚合对象数据
         */
        mDataList.addAll(arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0))
    }

    /**
     * 获取聚合对象元素个数
     */
    fun getCount() = mDataList.size

    /**
     * 获取聚合对象中对应位置元素
     */
    fun getItem(position: Int) = mDataList[position]

    /**
     * 创建迭代器对象，并将自身作为被委托对象传递给迭代器
     */
    override fun iterator(): IIterator<Int> = DelegateIterator(this)
}

/**
 * 创建委托形式的迭代器对象
 * 通过迭代器构造器将被委托聚合对象传递给委托迭代器对象，在迭代器中通过操作聚合对象实现迭代逻辑
 */
class DelegateIterator(val delegate: AggregateImpl2) : IIterator<Int> {

    /**
     * 迭代过程中index位
     */
    private var mIndex: Int = 0

    /**
     * 判断mDataList对象中mIndex位元素是否有效
     */
    override fun hasNext(): Boolean = (mIndex < delegate.getCount())

    /**
     * 获得mDataList对象中mIndex位元素对象
     */
    override fun next(): Int = delegate.getItem(mIndex++)
}
