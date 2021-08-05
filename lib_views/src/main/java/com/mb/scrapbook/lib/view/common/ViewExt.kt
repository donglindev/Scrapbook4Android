package com.mb.scrapbook.lib.view.common

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.children
import com.blankj.utilcode.util.SizeUtils

/**
 * 所有自定义View规范
 */
interface IView {
    /**
     * 初始化
     */
    fun onInitView(contract: ViewContract)

    /**
     * 绑定数据
     */
    fun onBindViewData(contract: ViewContract)

    /**
     * 是否含有子View
     */
    fun hasChild(): Boolean
}


/**
 * View扩展函数
 * 此扩展函数用于自定义View初始化和绑定数据
 */
fun View.onSetupView(contract: ViewContract?) {
    contract?.let {
        if (this is IView) {
            (this as IView).run {
                onInitView(contract)
                onLayoutView(contract)
                onBindViewData(contract)
            }
        }
    }
}

/**
 * View扩展函数
 * 此扩展函数用于自定义View更新绑定数据
 */
fun View.onUpdateView(contract: ViewContract?) {
    contract?.let {
        if (this is IView) {
            (this as IView).run {
                onLayoutView(contract)
                onBindViewData(contract)
            }
        }
    }
}

/**
 * View扩展函数
 * 此扩展函数用于配置View相关下发的属性
 */
fun View.onLayoutView(contract: ViewContract) {
    ViewContractRepository.findViewLayoutContract(contract.uniqueId)?.let {
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
 * View合约
 */
data class ViewContract(val uniqueId: Long,
                        val index: Int,
                        val type: Int,
                        val category: Int,
                        var resId: Int? = null)


/**
 * View布局合约
 */
data class ViewLayoutContract(val view: ViewContract,
                              val size: SizeConfig,
                              var margin: MarginConfig? = null,
                              var padding: PaddingConfig? = null,
                              var orientation: OrientationConfig? = null,
                              var position: PositionConfig? = null)

/**
 * View数据合约
 */
data class ViewDataContract(val view: ViewContract)

/**
 * 大小配置
 * width和height使用非标准单位：dp, sp, ..., ...
 */
data class SizeConfig(val width: Int, val height: Int)

/**
 * 位置配置
 */
data class PositionConfig(val x: Float, val y: Float,
                          val left: Float, val top: Float,
                          val right: Float, val bottom: Float)

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
