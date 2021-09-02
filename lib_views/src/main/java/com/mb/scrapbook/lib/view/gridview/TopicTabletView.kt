package com.mb.scrapbook.lib.view.gridview

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.blankj.utilcode.util.SizeUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.mb.scrapbook.lib.base.model.FloatSortHolder
import com.mb.scrapbook.lib.view.R
import com.mb.scrapbook.lib.view.model.ImageTextData

/**
 * 话题牌
 *
 * @author DongLin
 * @date 2021/08/26
 */
class TopicTabletView(context: Context, attr: AttributeSet): RecyclerView(context, attr) {

    // textSize="16sp"
    private val pxTextSize by lazy { SizeUtils.sp2px(16f) }

    companion object {
        const val SPAN_COUNT: Int = 2
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        /**
         * 初始化
         */
        adapter = Adapter()
        layoutManager = StaggeredGridLayoutManager(SPAN_COUNT, RecyclerView.HORIZONTAL)
        addItemDecoration(ItemDecorator())
    }

    /**
     * 更新数据
     */
    fun onUpdateData(data: MutableList<ImageTextData>) {
        adapter?.let {
            (it as Adapter).let {
                it.data.clear()
                it.data.addAll(sort(data))
                it.notifyDataSetChanged()
            }
        }
    }


    /**
     * 排序数据
     */
    private fun sort(data: MutableList<ImageTextData>) : List<ImageTextData> {
        return data.let {
            val paint = Paint()
            paint.textSize = pxTextSize.toFloat()
            data.map { FloatSortHolder(paint.measureText(it.textRes), it) }
                .sortedBy { it.sortKey }
                .map {
                    ImageTextData(imageResId = it.holder.imageResId, textRes = it.holder.textRes)
                }
        }
    }


    /**
     * 话题榜单分割线
     */
    private class ItemDecorator : RecyclerView.ItemDecoration() {

        // 15dp
        private val offsetRightPx by lazy { SizeUtils.dp2px(10f) }
        // 20dp
        private val offsetBottomPx by lazy { SizeUtils.dp2px(20f) }

        override fun getItemOffsets(outRect: Rect,
                                    view: View,
                                    parent: RecyclerView,
                                    state: State) {
            outRect.right = offsetRightPx
            outRect.bottom = offsetBottomPx

            val itemCount = parent.adapter?.run { itemCount - 1 } ?: 0
            val position = parent.getChildAdapterPosition(view)
            // 奇数
            if (position % 2 != 0) {
                outRect.bottom = 0
                if (itemCount == position) {
                    outRect.right = 0
                }
            }
            // 偶数最后一个（第一行最后一个）
            else if (position == (itemCount - 1)) {
                outRect.right = 0
            }
        }
    }


    /**
     * TopicTabletView adapter
     */
    private class Adapter(val data: MutableList<ImageTextData> = mutableListOf())
        : RecyclerView.Adapter<ViewHolder>()
    {

        override fun getItemCount(): Int = data.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
                    (holder as VHItem).onBind(data.get(position))

        override fun onCreateViewHolder(container: ViewGroup,
                                        viewType: Int): ViewHolder {
            return VHItem(LayoutInflater.from(container.context).inflate(
                R.layout.topic_tablet_item_view, container, false
            ))
        }
    }


    /**
     * TopicTabletView ViewHolder
     */
    private class VHItem : ViewHolder {

        private val image: ImageView
        private val text: TextView

        constructor(view: View): super(view) {
            image = view.findViewById(R.id.image)
            text = view.findViewById(R.id.text)
        }

        fun onBind(data: ImageTextData?) {
            data?.let {
                // 设置圆形图片
                Glide.with(image)
                    .load(it.imageResId)
                    .apply(RequestOptions.circleCropTransform())
                    .into(image)

                // 设置文字
                text.text = it.textRes
            }
        }
    }
}