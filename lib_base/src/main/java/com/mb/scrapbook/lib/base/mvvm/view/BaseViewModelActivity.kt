package com.mb.scrapbook.lib.base.mvvm.view

import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.mb.scrapbook.lib.base.mvvm.viewmodel.BaseViewModel
import com.mb.scrapbook.lib.base.utils.Util
import java.lang.reflect.ParameterizedType

/**
 * Base ViewModel Activity
 *
 * @author DongLin
 * @date 2021/07/29
 */
abstract class BaseViewModelActivity<VM: BaseViewModel<*>>: BaseActivity() {

    protected lateinit var mViewModel: VM


    /**
     * 初始化LiveData对象并进行绑定
     */
    abstract fun initDataObserver()


    override fun initView() {
        // 初始化ViewModel对象
        mViewModel = ViewModelProvider(this).get(Util.getClass(this))
        // 初始化LiveData对象并绑定
        initDataObserver()
    }
}