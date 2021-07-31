package com.mb.scrapbook.lib.view.gridview

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.mb.scrapbook.lib.view.indicator.DistanceIndicator

class HorizontalDistanceGridView(context: Context, set: AttributeSet) : ConstraintLayout(context, set) {

    private lateinit var rvGridView: RecyclerView
    private lateinit var lineIndicator: DistanceIndicator

    override fun onFinishInflate() {
        super.onFinishInflate()

    }
}