package com.mb.scrapbook.app.module.example.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mb.scrapbook.app.module.example.model.ExampleItemData
import com.mb.scrapbook.app.module.example.repository.ExampleRepository
import com.mb.scrapbook.lib.base.mvvm.viewmodel.BaseViewModel
import kotlinx.coroutines.launch

class ExampleViewModel: BaseViewModel<ExampleRepository>() {

    companion object {
        const val TAG = "ExampleViewModel"
    }

    /**
     * 列表主页数据LiveData
     */
    private var _mainLiveData: MutableLiveData<MutableList<ExampleItemData>> = MutableLiveData()
    val mainLiveData = _mainLiveData


    /**
     * 加载主页列表数据
     */
    fun loadMainList() {
        viewModelScope.launch {
            _mainLiveData.value = repository.reqMainList()
        }
    }


    /**
     * 加载子列表数据
     */
    fun loadChildList(groupType: Int) {
        viewModelScope.launch {
            _mainLiveData.value = repository.reqChildList(groupType)
        }
    }
}