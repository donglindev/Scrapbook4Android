package com.mb.scrapbook.app.module.example.repository

import androidx.lifecycle.MutableLiveData
import com.mb.scrapbook.app.module.example.lib.recyclerview.view.RecyclerViewFragment
import com.mb.scrapbook.app.module.example.model.ExampleItemData
import com.mb.scrapbook.lib.base.common.State
import com.mb.scrapbook.lib.base.model.ItemKey
import com.mb.scrapbook.lib.base.mvvm.repository.BaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

/**
 * Example Activity
 *
 * @author DongLin
 * @date 2021/08/02
 */
class ExampleRepository(private val state: MutableLiveData<State>): BaseRepository() {

    companion object {
        const val TYPE_HIDE_FRAGMENT: Int = 0x0
        const val TYPE_INDICATOR: Int = 0xA000
        const val TYPE_RECYCLER_VIEW: Int = 0xB000
    }
    // 数据
    private var mapData: MutableMap<ItemKey, MutableList<ExampleItemData>>


    init {
        // Key id 生成器
        val genKey = AtomicInteger();
        /**
         * 指示器项
         */
        var listIndicator: MutableList<ExampleItemData> = mutableListOf(
            ExampleItemData((TYPE_INDICATOR + genKey.incrementAndGet()),
                            name = "线性指示器",
                            groupType = TYPE_INDICATOR,
                            hasChild = false),

            ExampleItemData((TYPE_INDICATOR + genKey.incrementAndGet()),
                            name = "线性距离指示器",
                            groupType = TYPE_INDICATOR,
                            hasChild = false)
        )

        /**
         * RecyclerView项
         */
        var listRecyclerView: MutableList<ExampleItemData> = mutableListOf(
            ExampleItemData((TYPE_RECYCLER_VIEW + genKey.incrementAndGet()),
                            name = "水平瀑布流",
                            TYPE_RECYCLER_VIEW,
                            hasChild = false,
                            fragment = RecyclerViewFragment())
        )

        /**
         * 全部数据
         */
        mapData = mutableMapOf(
            Pair(ItemKey(TYPE_INDICATOR, "指示器"), listIndicator),
            Pair(ItemKey(TYPE_RECYCLER_VIEW, "RecyclerView"), listRecyclerView)
        )
    }


    /**
     * 列表首页数据
     *
     * 指示器
     * *****
     * *****
     */
    suspend fun reqMainList(): MutableList<ExampleItemData> {
        return withContext(Dispatchers.IO) {
            mapData.keys.map { ExampleItemData(it.id, it.name) }.toMutableList()
        }
    }


    /**
     * 子列表数据
     */
    suspend fun reqChildList(groupType: Int): MutableList<ExampleItemData> {
        return withContext(Dispatchers.IO) {
            val key: ItemKey? = findKey(groupType)
            mapData.get(key)?.filter {
                it.groupType == groupType
            }?.toMutableList() ?: mutableListOf()
        }
    }


    /**
     * 根据type查找ItemKey对象
     */
    private fun findKey(type: Int): ItemKey? {
        return mapData.keys.filter { it.id == type }?.get(0)
    }
}