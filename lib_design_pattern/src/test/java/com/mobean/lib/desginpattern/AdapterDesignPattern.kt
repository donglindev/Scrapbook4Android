package com.mobean.lib.desginpattern

import org.junit.Test

/**
 * Adapter Design Pattern
 *
 * 适配器模式
 * 1) 实现类适配器模式（继承）；
 * 2) 实现对象适配器模式（委托）；
 *
 * 假设“现有程序”已经存在一台打印机（Printer），它有两个操作
 * 1）输出表示加强带星号的内容，例如：*** Content ***
 * 2）输出表示弱化带括号的内容，例如：((( Content )))
 *
 * 另外“所需程序”是一台显示器（IDisplay），它支持强化和弱化两个显示操作；
 * 最终我们通过适配器模式实现将打印机（现有程序）在显示器（所需程序）上完美显示；
 *
 * 说明：由于现有程序和所需程序位于两个独立的库中，不能采用修改源码方式实现，所以采用适配器模式实现；
 */

/**
 * 所需程序-显示器
 */
interface IDisplay {
    /**
     * 使用弱化显示
     */
    fun displayWeak(content: String)

    /**
     * 使用强化显示
     */
    fun displayStrong(content: String)
}

/**
 * 现有程序-打印机
 */
interface IPrinter {
    /**
     * 输出表示弱化带括号的内容
     */
    fun showWithParen(content: String)

    /**
     * 输出表示强化带星号的内容
     */
    fun showWithAster(content: String)
}

/**
 * 现有程序-打印机
 */
open class Printer : IPrinter {
    /**
     * 输出表示弱化带括号的内容：(((content)))
     */
    override fun showWithParen(content: String) = println("((($content)))")

    /**
     * 输出表示强化带星号的内容:***content***
     */
    override fun showWithAster(content: String) = println("***$content***")
}

/**
 * 使用类适配器模式，实现打印机适配显示器功能
 */
class PrintAdapter : Printer(), IDisplay {
    /**
     * 使用弱化功能显示时
     */
    override fun displayWeak(content: String) = showWithParen(content)

    /**
     * 使用强化功能显示时
     */
    override fun displayStrong(content: String) = showWithAster(content)
}

/**
 * 适配器模式入口类
 */
class AdapterDesignPattern {

    /**
     * 类适配器模式入口方法（继承）
     */
    @Test fun demoPrintAdapter() {
        // 使用所需程序接口定义对象
        val display: IDisplay = PrintAdapter()
        display.displayWeak("Weakly") // (((Weakly)))
        display.displayStrong("Stronger") // ***Stronger***
    }

    /**
     * 对象适配器入口方法（委托）
     */
    @Test fun demoPrintAdapter2() {
        // 使用现有程序定义对象
        val printer = Printer2()
        printer.showWithParen("Weakly") // (Weakly)
        printer.showWithAster("Stronger") // *Stronger*
    }
}

/**
 * 假设我们对现有程序有修改源码权限，为了能够使用所需程序中高级显示器功能，
 * 使用对象适配器模式实现对高级显示器功能的调用
 */
/**
 * 实现对象适配器模式（委托）
 * 由于采用委托方式实现，所需程序需要具体实例
 */
class AdvancedDisplay : IDisplay {
    /**
     * 高级显示器可自己显示弱化内容
     */
    override fun displayWeak(content: String) = println("($content)")

    /**
     * 高级显示器可自己显示强化内容
     */
    override fun displayStrong(content: String) = println("*$content*")
}

/**
 * AdvancedAdapter适配器对象
 * 它与现有程序接口一致，并持有所需程序对象
 */
class AdvancedAdapter : IPrinter {
    /**
     * 所需程序对象
     */
    private val mDisplay: IDisplay by lazy { AdvancedDisplay() }

    /**
     * 与现有程序接口一致
     */
    override fun showWithParen(content: String) = mDisplay.displayWeak(content)

    /**
     * 与现有程序接口一致
     */
    override fun showWithAster(content: String) = mDisplay.displayStrong(content)
}

/**
 * 现有程序对象，有权限修改源码时，在此持有适配器对象
 */
class Printer2 : IPrinter {
    /**
     * 适配器对象（委托对象）
     */
    private val mAdapter: AdvancedAdapter by lazy { AdvancedAdapter() }

    /**
     * 使用弱化功能显示时
     */
    override fun showWithParen(content: String) = mAdapter.showWithParen(content)

    /**
     * 使用强化功能显示时
     */
    override fun showWithAster(content: String) = mAdapter.showWithAster(content)
}
