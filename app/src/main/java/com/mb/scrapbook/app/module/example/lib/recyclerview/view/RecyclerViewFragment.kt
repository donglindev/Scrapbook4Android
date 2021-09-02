package com.mb.scrapbook.app.module.example.lib.recyclerview.view

import android.view.LayoutInflater
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import com.mb.scrapbook.app.R
import com.mb.scrapbook.app.module.example.lib.recyclerview.model.ChannelItemTypeData
import com.mb.scrapbook.app.module.example.lib.recyclerview.repository.ChannelRepository.Companion.TYPE_STAGGERED_HORIZONTAL
import com.mb.scrapbook.app.module.example.lib.recyclerview.viewmode.RVFViewModel
import com.mb.scrapbook.lib.base.mvvm.view.BaseViewModelFragment
import com.mb.scrapbook.lib.view.gridview.TopicTabletView
import com.mb.scrapbook.lib.view.model.ImageTextData
import kotlinx.android.synthetic.main.layout_channel.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * RecyclerView样式展示类
 *
 * @author DongLin
 * @date 2021/08/26
 */
class RecyclerViewFragment: BaseViewModelFragment<RVFViewModel>() {

    // 设置布局文件
    override fun getLayoutId(): Int = R.layout.layout_channel

    /**
     * 监听LiveData数据
     */
    override fun initDataObserver() {
        mViewModel.channelLiveData.observe(this) {
            /*
             * 监听频道数据变化
             */
            onLoadChannelDataCompleted(it)
        }
        mViewModel.topicTabletLiveData.observe(this) {
            /*
             * 监听话题榜单数据变化
             */
            onLoadTopicTabletDataCompleted(it)
        }
    }

    /**
     * 初始化数据
     */
    override fun onInitData() {
        // 请求频道数据
        mViewModel.loadChannelData()
    }

    /**
     * 初始化频道数据
     */
    private fun onLoadChannelDataCompleted(
                        dataChannel: MutableList<ChannelItemTypeData>
    ) {
        dataChannel.apply {
            lifecycleScope.launch(Dispatchers.Main) {
                val inflater = LayoutInflater.from(context)
                forEach() {
                    val view = withContext(Dispatchers.IO) {
                        it.resId?.let {
                            inflater.inflate(it, null, false)
                        } ?: null
                    }
                    if (view != null) {
                        view.tag = it.resType
                        layoutContainer.addView(view)
                        when (it.resType) {
                            TYPE_STAGGERED_HORIZONTAL -> {
                                mViewModel.loadTopicTabletData()
                            }
                        }
                    }
                }
            }
        }
    }


    /**
     * 初始化话题榜单数据
     */
    private fun onLoadTopicTabletDataCompleted(
                        dataTopicTablet: MutableList<ImageTextData>
    ) {
        lifecycleScope.launch(Dispatchers.Main) {
            for (index in 0 until layoutContainer.childCount) {
                layoutContainer.get(index)?.let {
                    if (TYPE_STAGGERED_HORIZONTAL == (it.tag as? Int)) {
                        (it as? TopicTabletView)?.onUpdateData(dataTopicTablet)
                    }
                }
            }
        }
    }

}