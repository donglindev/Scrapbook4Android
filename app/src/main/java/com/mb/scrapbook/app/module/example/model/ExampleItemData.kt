package com.mb.scrapbook.app.module.example.model

/**
 * Example Item Data
 * 列表页条目显示
 *
 * @author DongLin
 * @date 2021/08/02
 */
data class ExampleItemData(val type: Int,
                           val name: String,
                           var groupType: Int = 0,
                           var hasChild: Boolean)
