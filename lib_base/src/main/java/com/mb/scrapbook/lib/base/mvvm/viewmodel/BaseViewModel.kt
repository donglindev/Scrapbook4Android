package com.mb.scrapbook.lib.base.mvvm.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mb.scrapbook.lib.base.common.State
import com.mb.scrapbook.lib.base.mvvm.repository.BaseRepository
import com.mb.scrapbook.lib.base.utils.Util

/**
 * Base ViewModel
 * 全局ViewModel父类，持有一个BaseRepository对象，和一个全局LiveData状态对象
 *
 * @author DongLin
 * @date 2021/07/28
 */
open class BaseViewModel<T: BaseRepository>: ViewModel() {

    /**
     * 全局状态LiveData监听
     */
    val state by lazy { MutableLiveData<State>() }


    /**
     * 通过反射创建对应的repository对象
     */
    val repository: T? by lazy {
        (Util.getClass<T>(this))
            .getDeclaredConstructor(MutableLiveData::class.java)
            .newInstance(state);
    }

}