package com.mb.scrapbook.lib.view.gridview

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleObserver
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.mb.scrapbook.lib.view.R
import com.mb.scrapbook.lib.view.common.IView
import com.mb.scrapbook.lib.view.common.ViewItemConfigure
import com.mb.scrapbook.lib.view.indicator.DistanceIndicator

/**
 * GridView＋距离指示器
 *
 * @author DongLin
 * @date 2021/08/04
 */
class HorizontalDistanceGridView(context: Context, set: AttributeSet)
                        : ConstraintLayout(context, set), IView, LifecycleObserver
{

    private lateinit var rvGridView: RecyclerView
    private lateinit var lineIndicator: DistanceIndicator

    /**
     * 数据源
     */
    private val dataAdapter: DataAdapter = DataAdapter(mutableListOf())


    /**
     * 初始化View属性
     */
    override fun onFinishInflate() {
        super.onFinishInflate()

    }


    /**
     * 更新数据
     */
    fun updateViewConfigureData(data: List<ViewItemConfigure>) {
    }


    /**
     * 数据源
     */
    private class DataAdapter(data: MutableList<ViewItemConfigure>)
                        : BaseQuickAdapter<ViewItemConfigure, BaseViewHolder>(
        R.layout.mb_recycler_view_item_view, data
    ) {

        override fun convert(holder: BaseViewHolder, item: ViewItemConfigure) {
        }

    }

}
