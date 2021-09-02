package com.mb.scrapbook.app.module.example.lib.recyclerview.viewmode

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mb.scrapbook.app.module.example.lib.recyclerview.model.ChannelItemTypeData
import com.mb.scrapbook.app.module.example.lib.recyclerview.repository.ChannelRepository
import com.mb.scrapbook.app.module.example.lib.recyclerview.repository.TopicTabletRepository
import com.mb.scrapbook.lib.base.common.State
import com.mb.scrapbook.lib.base.mvvm.viewmodel.BaseViewModel
import com.mb.scrapbook.lib.view.model.ImageTextData
import kotlinx.coroutines.launch

/**
 * RecyclerView Fragment ViewModel
 *
 * @author DongLin
 * @date 2021/08/25
 */
class RVFViewModel : BaseViewModel<ChannelRepository>() {

    // 话题榜单数据仓库
    private val repositoryTopicTablet by lazy { TopicTabletRepository(MutableLiveData<State>()) }

    /**
     * 频道页数据LiveData
     */
    private var _channelLiveData: MutableLiveData<MutableList<ChannelItemTypeData>> = MutableLiveData()
    val channelLiveData = _channelLiveData


    /**
     * 话题榜单数据LiveData
     */
    private var _topicTabletLiveData: MutableLiveData<MutableList<ImageTextData>> = MutableLiveData()
    val topicTabletLiveData = _topicTabletLiveData


    /**
     * 加载话题榜单数据
     */
    fun loadTopicTabletData() {
        viewModelScope.launch {
            _topicTabletLiveData.value = repositoryTopicTablet.reqTopicTabletData()
        }
    }


    /**
     * 加载频道数据
     */
    fun loadChannelData() {
        viewModelScope.launch {
            _channelLiveData.value = repository.reqChannelData()
        }
    }
}