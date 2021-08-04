package com.mb.scrapbook.app.module.example.model

import androidx.fragment.app.Fragment

/**
 * Example Item Data
 * 列表页条目显示
 *
 * @author DongLin
 * @date 2021/08/02
 */
data class ExampleItemData(val type: Int,
                           var name: String = "",
                           var groupType: Int = 0,
                           var hasChild: Boolean = true,
                           var example: String = "",
                           var fragment: Fragment? = null)
