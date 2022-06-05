package com.mb.scrapbook.app.module.example.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mb.scrapbook.app.module.example.model.ExampleItemData
import com.mb.scrapbook.app.module.example.repository.ExampleRepository
import com.mb.scrapbook.lib.base.mvvm.viewmodel.BaseViewModel
import kotlinx.coroutines.launch
import java.util.*

class ExampleViewModel: BaseViewModel<ExampleRepository>() {

    companion object {
        const val TAG = "ExampleViewModel"
    }

    /**
     * 显示栈
     * 当前界面显示的内容，若栈为空表示可退出，否则返回上一级ExampleItemData对象；
     */
    private val stackDisplay = LinkedList<ExampleItemData>()

    /**
     * 列表主页数据LiveData
     */
    private var _mainLiveData: MutableLiveData<MutableList<ExampleItemData>> = MutableLiveData()
    val mainLiveData = _mainLiveData

    /**
     * 校验显示栈有效性
     */
    fun shouldBack(): Boolean = stackDisplay.peek()?.let {
        val item = stackDisplay.pop()
        if (!item.hasChild) { // 显示具体示例Fragment内容
            displayListContainer()
        } else if (item.groupType == 0) { // 已经回退到根节点
            loadMainList()
        }
        true
    } ?: false


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
    fun loadChildList(data: ExampleItemData) {
        viewModelScope.launch {
            stackDisplay.push(data) // 显示栈
            _mainLiveData.value = repository.reqChildList(data.type)
        }
    }


    /**
     * Example界面上Fragment状态LiveData
     */
    private var _exampleLiveData: MutableLiveData<ExampleItemData> = MutableLiveData()
    val exampleLiveData = _exampleLiveData


    /**
     * 根据ExampleItemData向Fragment填充数据
     */
    fun displayExampleContainer(data: ExampleItemData) {
        stackDisplay.push(data) // 显示栈
        exampleLiveData.value = data
    }


    /**
     * 隐藏Example Fragment显示RecyclerView List
     */
    private fun displayListContainer() {
        exampleLiveData.value = ExampleItemData(ExampleRepository.TYPE_HIDE_FRAGMENT)
    }
}