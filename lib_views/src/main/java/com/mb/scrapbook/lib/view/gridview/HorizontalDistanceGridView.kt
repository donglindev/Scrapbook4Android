package com.mb.scrapbook.lib.view.gridview

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleObserver
import androidx.recyclerview.widget.RecyclerView
import com.mb.scrapbook.lib.view.indicator.DistanceIndicator

/**
 * GridView＋距离指示器
 *
 * @author DongLin
 * @date 2021/08/04
 */
class HorizontalDistanceGridView(context: Context, set: AttributeSet)
                        : ConstraintLayout(context, set), LifecycleObserver
{

    private lateinit var rvGridView: RecyclerView
    private lateinit var lineIndicator: DistanceIndicator


    /**
     * 初始化View属性
     */
    override fun onFinishInflate() {
        super.onFinishInflate()

    }

}
