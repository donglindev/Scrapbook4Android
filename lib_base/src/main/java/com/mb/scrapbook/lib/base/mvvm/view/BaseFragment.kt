package com.mb.scrapbook.lib.base.mvvm.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

/**
 * Base Fragment
 *
 * @author DongLin
 * @date 2021/07/29
 */
abstract class BaseFragment: Fragment() {

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
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(getLayoutId(), container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 设置status bar样式
        setupStatusBarStyle()
        // 初始化布局
        onInitView()
        // 初始化数据
        onInitData()
    }
}