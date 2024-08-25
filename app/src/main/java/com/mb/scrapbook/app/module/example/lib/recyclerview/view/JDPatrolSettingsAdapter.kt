package com.mb.scrapbook.app.module.example.lib.recyclerview.view

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import androidx.core.view.marginBottom
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ColorUtils
import com.blankj.utilcode.util.SizeUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.mb.scrapbook.app.R
import com.mb.scrapbook.app.module.example.lib.recyclerview.model.ItemSettingsData
import com.mb.scrapbook.app.module.example.lib.recyclerview.model.JDPatrolSettingsUtils
import com.mb.scrapbook.app.module.example.lib.recyclerview.model.SettingsValue.TextSettings
import com.mb.scrapbook.app.module.example.lib.recyclerview.model.SettingsValue.ButtonSettings
import com.mb.scrapbook.app.module.example.lib.recyclerview.repository.JDPatrolSettingsRepository
import com.mb.scrapbook.app.module.example.lib.recyclerview.viewmode.JDPatrolSettingsViewModel
import com.mb.scrapbook.lib.base.model.ItemKey
import com.thekhaeng.recyclerviewmargin.LayoutMarginDecoration
import kotlinx.coroutines.launch

/**
 * JD巡检设置数据适配器
 *
 * @author donglin6
 * @date 2024/06/03
 */
class JDPatrolSettingsAdapter(data: MutableList<ItemKey>,
                              private val viewModel: JDPatrolSettingsViewModel)
                : BaseQuickAdapter<ItemKey, BaseViewHolder>(
        R.layout.rv_jd_patrol_settings_adapter_item, data
) {

    /** 每一项 */
    private inner class ItemAdapter(data: MutableList<ItemSettingsData>)
                : BaseQuickAdapter<ItemSettingsData, BaseViewHolder>(
        R.layout.rv_jd_patrol_settings_child_item, data
    ) {
        /** 设置巡检项中的子项内容(内层) */
        override fun convert(holder: BaseViewHolder, item: ItemSettingsData) {
            holder.setVisible(R.id.progressbar, true)
            // 1、设置标题
            holder.setText(R.id.title, item.value.title)
            // 2、设置组件
            JDPatrolSettingsUtils.searchConfigValue(item)?.apply {
                holder.setVisible(R.id.progressbar, false)
                when (item.uniqueType) {
                    // 系统版本
                    JDPatrolSettingsRepository.TYPE_SYSTEM_DEVICE_NAME -> {
                        holder.setVisible(R.id.displayText, true)
                        holder.setText(R.id.displayText, (item.value as TextSettings).displayText())
                    }
                    // 权限状态
                    JDPatrolSettingsRepository.TYPE_SYSTEM_PERMISSION_STATE,
                    // 巡检服务
                    JDPatrolSettingsRepository.TYPE_SYSTEM_PATROL_SERVICE -> {
                        holder.setVisible(R.id.button, true)
                        val settings = (item.value as ButtonSettings)
                        holder.setText(R.id.button, settings.displayText())
                        holder.getView<View>(R.id.button).setOnClickListener(settings.getClickListenerImpl())
                        if (settings.clickable()) {
                            holder.setTextColor(R.id.button, Color.RED)
                        } else {
                            holder.setTextColor(R.id.button, Color.BLACK)
                        }
                    }
                }
            }
        }

    }

    /** 设置巡检项容器组件(外层) */
    @SuppressLint("NotifyDataSetChanged")
    override fun convert(holder: BaseViewHolder, item: ItemKey) {
        // 1）设置标题
        holder.setText(R.id.title, item.name)
        // 2）设置内部RecyclerView
        holder.getView<RecyclerView>(R.id.rvDataList).apply {
            // 通过RecyclerView中adapter对象是否为null值防止多次初始化
            if (adapter == null) {
                layoutManager = object : LinearLayoutManager(context) {
                    init { orientation = RecyclerView.VERTICAL }
                    override fun canScrollVertically(): Boolean = false
                }
                adapter = ItemAdapter(viewModel.searchDataListByKey(item))
                (adapter as ItemAdapter).setEmptyView(com.mb.scrapbook.lib.view.R.layout.view_empty_loading)
                // 绘制灰色线
                val spacingSize = SizeUtils.dp2px(0.5f)
                addItemDecoration(object:LayoutMarginDecoration(1, spacingSize) {
                    private val mPaint = Paint() // 分割线画笔
                    init {
                        mPaint.strokeWidth = spacingSize.toFloat()
                        mPaint.isAntiAlias = true
                        mPaint.style = Paint.Style.STROKE
                        mPaint.color = ColorUtils.string2Int("#F6F6F6")
                    }
                    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                        super.onDraw(canvas, parent, state)
                        for (idx in 0 until parent.childCount) {
                            val view = parent.getChildAt(idx)
                            val top = (view.bottom + view.marginBottom)
                            val bottom = top + spacingSize
                            canvas.drawRect(parent.paddingLeft.toFloat(), top.toFloat(),
                                    parent.width.toFloat(), bottom.toFloat(), mPaint)
                        }
                    }
                })

                // 根据key加载对应的子RecyclerView数据，同时更新子RecyclerView数据；
                viewModel.viewModelScope.launch {
                    val childDataList = viewModel.loadPatrolDataListByKey(item)
                    (adapter as ItemAdapter).setList(childDataList)
                    viewModel.initPatrolConfigureList(item, context) // 初始化配置项
                }
            } else {
                (adapter as ItemAdapter).setList(viewModel.searchDataListByKey(item))
            }
        }
    }

}