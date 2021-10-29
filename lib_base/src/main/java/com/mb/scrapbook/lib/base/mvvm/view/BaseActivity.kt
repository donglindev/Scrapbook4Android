package com.mb.scrapbook.lib.base.mvvm.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.BarUtils

/**
 * Base Activity
 * 标准Activity基类
 *
 * @author DongLin
 * @date 2021/07/29
 */
abstract class BaseActivity: AppCompatActivity() {

    /**
     * 子类实现
     */
    abstract fun getLayoutId(): Int

    /**
     * 子类实现View初始化
     */
    abstract fun onInitView()

    /**
     * 子类实现数据初始化
     */
    abstract fun onInitData()

    /**
     * 设置status bar样式
     */
    protected open fun setupStatusBarStyle() {
        BarUtils.setStatusBarColor(this, ContextCompat.getColor(this, android.R.color.white))
        BarUtils.setStatusBarLightMode(this, true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 设置布局
        setContentView(getLayoutId())
        // 设置stats bar样式
        setupStatusBarStyle()
        // 初始化布局
        onInitView()
        // 初始化数据
        onInitData()
    }
}