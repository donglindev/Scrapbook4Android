package com.mobean.lib.desginpattern

import org.junit.Test

/**
 * Factory Method Pattern
 *
 * 工厂方法模式
 * 实现身份证创建和注册流程
 */

/**
 * 定义产品接口
 */
interface Card {
    /**
     * 使用卡片
     */
    fun useCard()
}

/**
 * 定义具体产品类
 */
data class IdCard(val owner: String) : Card {
    /**
     * 实现使用卡片
     */
    override fun useCard() = println("$owner used id card.")
}

/**
 * 定义工厂接口
 */
abstract class Factory {
    /**
     * 定义创建卡片抽象方法
     */
    abstract fun createCard(owner: String): Card

    /**
     * 定义注册卡片抽象方法
     */
    abstract fun registerCard(card: Card): Unit

    /**
     * 模板方法
     * 创建卡片和注册流程
     */
    fun create(owner: String) : Card {
        val card = createCard(owner)
        registerCard(card)
        return card
    }
}

/**
 * 定义具体工厂类
 * 实现身份证创建和注册流程
 */
class IdCardFactory(): Factory() {
    /**
     * 使用IdCardFactory创建身份证信息会存储在此集合中
     */
    private val mOwnerData by lazy { mutableListOf<Card>() }

    /**
     * 创建身份证信息
     */
    override fun createCard(owner: String): Card = IdCard(owner)

    /**
     * 注册身份证信息
     */
    override fun registerCard(card: Card) {
        mOwnerData.add(card)
    }
}

/**
 * 工厂方法模式入口类
 */
class FactoryMethodPattern {

    /**
     * 工厂方法模式入口方法
     */
    @Test fun demoCreateIdCard() {
        val ownerArray = listOf<String>("张三", "李四", "王五")
        // 创建具体工厂对象
        val factory: Factory = IdCardFactory()
        // 创建具体产品对象
        ownerArray.forEach { name ->
            val owner = factory.create(name)
            owner.useCard() // 张三 used id card.
                            // 李四 used id card.
                            // 王五 used id card.
        }
    }
}