package com.mb.scrapbook.app.module.example.view

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.mb.scrapbook.app.R
import com.mb.scrapbook.app.module.example.model.ExampleItemData
import com.mb.scrapbook.app.module.example.viewmodel.ExampleViewModel

/**
 * Example Item Adapter
 * 使用BRVAH展示RecyclerView数据
 *
 * @author DongLin
 * @date 2021/08/02
 */
class ExampleItemAdapter(data: MutableList<ExampleItemData>,
                         private val viewMode: ExampleViewModel)
                    : BaseQuickAdapter<ExampleItemData, BaseViewHolder>(
    // 布局和数据
    R.layout.rv_item_example_line, data
) {

    /**
     * 设置每一项数据
     */
    override fun convert(holder: BaseViewHolder, item: ExampleItemData) {
        holder.setText(R.id.text, item.name)
        if (item.hasChild) {
            holder.itemView.setOnClickListener {
                viewMode.loadChildList(item.type)
            }
        }
    }

}