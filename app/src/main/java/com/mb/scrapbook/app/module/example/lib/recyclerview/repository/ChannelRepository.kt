package com.mb.scrapbook.app.module.example.lib.recyclerview.repository

import androidx.lifecycle.MutableLiveData
import com.mb.scrapbook.app.R
import com.mb.scrapbook.app.module.example.lib.recyclerview.model.ChannelItemTypeData
import com.mb.scrapbook.lib.base.common.State
import com.mb.scrapbook.lib.base.mvvm.repository.BaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 频道数据仓库
 *
 * @author DongLin
 * @date 2021/08/25
 */
class ChannelRepository(private val state: MutableLiveData<State>) : BaseRepository() {

    companion object {
        const val TYPE_STAGGERED_HORIZONTAL: Int = 0x001;
    }

    // 数据集
    private val dataItemType: MutableList<ChannelItemTypeData> = mutableListOf(
        ChannelItemTypeData(TYPE_STAGGERED_HORIZONTAL, R.layout.mb_topic_tablet_view)
    )


    /**
     * 频道列表页数据
     */
    suspend fun reqChannelData(): MutableList<ChannelItemTypeData> {
        return withContext(Dispatchers.IO) {
            dataItemType.toMutableList()
        }
    }

}