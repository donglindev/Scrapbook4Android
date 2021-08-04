package com.mb.scrapbook.lib.view.common

import android.widget.LinearLayout


/**
 * ViewItem配置项
 *
 * configRelation: View的位置配置，包括：方向、Image存放的位置、Text存放的位置
 * configMargin: View的外边距
 * configPadding: View的内边距
 * content: View的内容，Image是前景图片地址，Text是文本内容
 */
data class ViewItemConfigure(private val configRelation: PositionalRelation,
                             private var configMargin: MarginPaddingConfigure? = null,
                             private var configPadding: MarginPaddingConfigure? = null,
                             private val content: String)


/**
 * View边距配置
 */
data class MarginPaddingConfigure(var left: Int = 0, var right: Int = 0,
                                  var top: Int = 0, var bottom: Int = 0)


/**
 * 位置配置项
 */
data class PositionalRelation(val orientation: Int,
                              val imagePosition: Int,
                              val textPosition: Int) {

    companion object {
        /**
         * orientation: HORIZONTAL/VERTICAL
         */
        const val HORIZONTAL = LinearLayout.HORIZONTAL
        const val VERTICAL = LinearLayout.VERTICAL

        /**
         * view positions
         */
        const val LEFT      = 0xA
        const val TOP       = 0xB
        const val RIGHT     = 0xC
        const val BOTTOM    = 0xD
    }
}
