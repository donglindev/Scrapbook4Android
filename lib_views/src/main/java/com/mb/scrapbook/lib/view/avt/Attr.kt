package com.mb.scrapbook.lib.view.avt

import android.widget.LinearLayout

/**
 * 组件树节点属性密封类
 * 此类用于描述组件支持的属性，例如组件必须支持尺寸，方向等属性
 *
 * @author Amit
 * @date 2021/12/26
 */
sealed class Attr {

    /**
     * 属性常量类
     */
    companion object {
        /** 属性Int类型默认值 */
        const val DEFAULT_INT = 0
        /** 属性Int类型无效值 */
        const val INVALIDATE_INT = -1

        /** 填满父组件 */
        const val MATCH_PARENT = "match_parent";
        /** 参考内容 */
        const val WRAP_CONTENT = "wrap_content";

        /** 水平 */
        const val HORIZONTAL = LinearLayout.HORIZONTAL
        /** 垂直 */
        const val VERTICAL = LinearLayout.VERTICAL
    }

    /**
     * 组件大小属性
     *
     * @param layoutWidth: match_parent | wrap_content
     * @param layoutHeight: match_parent | wrap_content
     * @param width: 组件的宽
     * @param height: 组件的高
     */
    data class Size(
        val layoutWidth: String? = MATCH_PARENT,
        val layoutHeight: String? = WRAP_CONTENT,
        val width: Int = DEFAULT_INT,
        val height: Int = DEFAULT_INT
    ) : Attr()

    /**
     * 组件绝对位置属性
     * 此属性描述一个组节点相对屏幕的绝对位置，任何浮层组件必须依附一个组节点组件
     *
     * @param x: 屏幕坐标系水平位置，默认-1无效
     * @param y: 屏幕坐标系垂直位置，默认-1无效
     * @param left: 距屏幕左边距，默认0边距
     * @param top: 距屏幕上边距，默认0边距
     * @param right: 距屏幕右边距，默认0边距
     * @param bottom: 距屏幕下边距，默认0边距
     */
    data class Position(
        val x: Int = INVALIDATE_INT,
        val y: Int = INVALIDATE_INT,
        val left: Int = DEFAULT_INT,
        val top: Int = DEFAULT_INT,
        val right: Int = DEFAULT_INT,
        val bottom: Int = DEFAULT_INT
    ) : Attr()

    /**
     * 组件方向属性
     * 当方向属性值为null时，此组表示FrameLayout组件，否则表示带方向的LinearLayout组件
     *
     * @param orientation: 组件方向，默认使用FrameLayout组件
     */
    data class Orientation(
        val orientation: Int? = null
    ) : Attr()

    /**
     * 组件外边距属性
     *
     * @param left: 距屏幕左边距，默认0边距
     * @param top: 距屏幕上边距，默认0边距
     * @param right: 距屏幕右边距，默认0边距
     * @param bottom: 距屏幕下边距，默认0边距
     */
    data class Margin(
        val left: Int = DEFAULT_INT,
        val top: Int = DEFAULT_INT,
        val right: Int = DEFAULT_INT,
        val bottom: Int = DEFAULT_INT
    ) : Attr()

    /**
     * 组件内边距属性
     *
     * @param left: 距屏幕左边距，默认0边距
     * @param top: 距屏幕上边距，默认0边距
     * @param right: 距屏幕右边距，默认0边距
     * @param bottom: 距屏幕下边距，默认0边距
     */
    data class Padding(
        val left: Int = DEFAULT_INT,
        val top: Int = DEFAULT_INT,
        val right: Int = DEFAULT_INT,
        val bottom: Int = DEFAULT_INT
    ) : Attr()

    /**
     * 组件内容对齐方式
     *
     * @param gravity: 通过组合方式实现内容对齐，默认左对齐
     */
    data class Gravity(
        val gravity: String = "start"
    ) : Attr()

    /**
     * 组件背景属性
     *
     * @param color: 组件背景色
     * @param image: 组件背景图
     */
    data class Background(
        val color: String? = null,
        val image: String? = null
    ) : Attr()

    /**
     * 组件圆角属性
     *
     * @param leftTop: 左上角，默认0度
     * @param leftBottom: 左下角，默认0度
     * @param rightTop: 右上角，默认0度
     * @param rightBottom: 右下角，默认0度
     */
    data class Corner(
        val leftTop: Int = 0,
        val leftBottom: Int = 0,
        val rightTop: Int = 0,
        val rightBottom: Int = 0
    ) : Attr()
}