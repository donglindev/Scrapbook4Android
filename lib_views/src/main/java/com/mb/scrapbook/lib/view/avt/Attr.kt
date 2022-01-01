package com.mb.scrapbook.lib.view.avt

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
        /** 水平 */
        const val HORIZONTAL = 0
        /** 垂直 */
        const val VERTICAL = 1
    }

    /**
     * 组件大小属性
     *
     * @param width: 组件的宽
     * @param height: 组件的高
     */
    data class Size(
        val width: Int = DEFAULT_INT,
        val height: Int = DEFAULT_INT
    ) : Attr()

    /**
     * 组件位置属性
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
     *
     * @param orientation: 组件方向，默认垂直方向
     */
    data class Orientation(
        val orientation: Int = VERTICAL
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
}