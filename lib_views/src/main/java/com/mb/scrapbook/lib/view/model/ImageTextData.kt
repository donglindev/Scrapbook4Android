package com.mb.scrapbook.lib.view.model

/**
 * 持有Image和Text数据
 *
 * @author DongLin
 * @date 2021/08/26
 */
data class ImageTextData(var imageResId: Int? = null,
                         var imageResUrl: String? = null,
                         var textResId: Int? = null,
                         var textRes: String? = null)
