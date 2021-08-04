package com.mb.scrapbook.lib.view.common

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.mb.scrapbook.lib.view.R

/**
 * RecyclerView中每一项子View
 *
 * @author DongLin
 * @date 2021/08/04
 */
class ViewItem(context: Context, set: AttributeSet) : LinearLayout(context, set), IView {

    /**
     * ImageView子项
     */
    private var image: ImageView? = null

    /**
     * TextView子项
     */
    private var text: TextView? = null


    /**
     * 初始化和设置子View
     */
    override fun onFinishInflate() {
        super.onFinishInflate()

        onInitView() // 初始化子View
    }


    /**
     * 初始化子View
     */
    override fun onInitView() {
        image = findViewById(R.id.image)
        text = findViewById(R.id.text)
    }


    /**
     * 子view绑定数据
     */
    override fun <T : IViewData> onBindViewData(data: T) {
    }

}