package com.mb.scrapbook.app.module.example.lib.recyclerview.repository

import androidx.lifecycle.MutableLiveData
import com.mb.scrapbook.app.R
import com.mb.scrapbook.lib.base.common.State
import com.mb.scrapbook.lib.base.mvvm.repository.BaseRepository
import com.mb.scrapbook.lib.view.model.ImageTextData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 话题榜单数据仓库
 *
 * @author DongLin
 * @date 2021/08/26
 */
class TopicTabletRepository(private val state: MutableLiveData<State>) : BaseRepository() {

    private val dataTopicTablet: MutableList<ImageTextData> = mutableListOf(
        ImageTextData(imageResId = R.drawable.ic_pic, textRes = "#热卖香水榜"),
        ImageTextData(imageResId = R.drawable.ic_pic, textRes = "#热卖防晒榜"),
        ImageTextData(imageResId = R.drawable.ic_pic, textRes = "#热卖面膜榜"),
        ImageTextData(imageResId = R.drawable.ic_pic, textRes = "#热卖精华推荐榜"),
        ImageTextData(imageResId = R.drawable.ic_pic, textRes = "#热卖面膜推荐榜"),
        ImageTextData(imageResId = R.drawable.ic_pic, textRes = "#热卖包包榜"),
        ImageTextData(imageResId = R.drawable.ic_pic, textRes = "#热卖防晒推荐榜"),
        ImageTextData(imageResId = R.drawable.ic_pic, textRes = "#热卖榜"),
        ImageTextData(imageResId = R.drawable.ic_pic, textRes = "#榜"),
        ImageTextData(imageResId = R.drawable.ic_pic, textRes = "#热卖电动牙刷推荐榜"),
    )

    /**
     * 热卖榜单数据
     */
    suspend fun reqTopicTabletData() : MutableList<ImageTextData> {
        return withContext(Dispatchers.IO) {
            dataTopicTablet.toMutableList()
        }
    }

}