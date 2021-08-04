package com.mb.scrapbook.lib.view.common

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.blankj.utilcode.util.SizeUtils


/**
 * 所有自定义View规范
 */
interface IView {

    /**
     * 初始化
     */
    fun onInitView()

    /**
     * 绑定数据
     */
    fun <T: IViewData> onBindViewData(data: T)
}

/**
 * 所有自定义View的数据基类
 */
abstract class IViewData(val uniqueId: Int)


/**
 * View扩展函数
 * 此扩展函数用于配置View相关下发的属性
 */
fun View.onConfigView(config: IViewConfigure?) {
    config?.let {
        /*
         * 设置View尺寸
         */
        layoutParams = with(layoutParams) {
            var pxWidth = width
            if (width > 0) {
                pxWidth = SizeUtils.dp2px(width.toFloat())
            }
            var pxHeight = height
            if (height > 0) {
                pxHeight = SizeUtils.dp2px(height.toFloat())
            }

            /*
             * 设置View外边距（Margin）
             */
            val marginParams = ViewGroup.MarginLayoutParams(pxWidth, pxHeight).apply {
                val marginLeft = (it.margin?.left ?: 0).toFloat()
                val marginTop = (it.margin?.top ?: 0).toFloat()
                val marginRight = (it.margin?.right ?: 0).toFloat()
                val marginBottom = (it.margin?.bottom ?: 0).toFloat()
                setMargins((if (marginLeft > 0) SizeUtils.dp2px(marginLeft) else 0),
                           (if (marginTop > 0) SizeUtils.dp2px(marginTop) else 0),
                           (if (marginRight > 0) SizeUtils.dp2px(marginRight) else 0),
                           (if (marginBottom > 0) SizeUtils.dp2px(marginBottom) else 0))
            }
            marginParams // 返回值
        }

        /*
         * 设置View内边距（Padding）
         */
        it.padding?.let {
            setPadding((if (left > 0) SizeUtils.dp2px(left.toFloat()) else 0),
                       (if (top > 0) SizeUtils.dp2px(top.toFloat()) else 0),
                       (if (right > 0) SizeUtils.dp2px(right.toFloat()) else 0),
                       (if (bottom > 0) SizeUtils.dp2px(bottom.toFloat()) else 0))
        }

        /*
         * 设置View方向（Orientation）
         */
        if (this is LinearLayout) {
            it.orientation?.let {
                this.orientation = orientation
            }
        }
    }
}


/**
 * 所有自定义View的配置基类
 */
data class IViewConfigure(val size: SizeConfig,
                          var margin: MarginConfig? = null,
                          var padding: PaddingConfig? = null,
                          var orientation: OrientationConfig? = null)


/**
 * 大小配置
 * width和height使用非标准单位：dp, sp, ..., ...
 */
data class SizeConfig(val width: Int, val height: Int)

/**
 * 位置配置
 */
data class PositionConfig(val position: Int)

/**
 * 方向配置
 */
data class OrientationConfig(val orientation: Int)

/**
 * 外边距配置
 */
data class MarginConfig(val left: Int, val top: Int, val right: Int, val bottom: Int)

/**
 * 内边距配置
 */
data class PaddingConfig(val left: Int, val top: Int, val right: Int, val bottom: Int)
