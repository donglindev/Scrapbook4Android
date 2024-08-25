/** Accessibility 数据文件 */
package com.mb.scrapbook.lib.base.model

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import java.lang.StringBuilder

/** 扩展函数：点击事件 */
fun AccessibilityNodeInfo.click() = performAction(AccessibilityNodeInfo.ACTION_CLICK)

/** 扩展函数：长按事件 */
fun AccessibilityNodeInfo.longClick() = performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)

/** 扩展函数：继续滑动 */
fun AccessibilityNodeInfo.scrollForward() = performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)

/** 扩展函数：回退滑动 */
fun AccessibilityNodeInfo.scrollBackward() = performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)

/** 扩展函数：文本填充 */
fun AccessibilityNodeInfo.input(content: String) = performAction(
    AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, content)
    }
)

/**
 * 扩展函数
 * 根据文本（content）查找带有此文本的全部节点并封装在集合中；
 * @return 返回的集合可能为Empty值
 */
fun AccessibilityNodeInfo.findNodeListByText(content: String) : List<AccessibilityNodeInfo> = findAccessibilityNodeInfosByText(content)

/**
 * 扩展函数
 * 根据文本（content）查找带有此文本的第一个节点并返回；
 * @return 返回的节点对象可能为Null值
 */
fun AccessibilityNodeInfo.findNodeByText(content: String) : AccessibilityNodeInfo? {
    val dataList = findNodeListByText(content)
    if (dataList.isNotEmpty()) {
        return dataList[0]
    }
    return null
}

/**
 * 扩展函数
 * 根据viewId查找全部节点并封装在集合中；
 * @return 返回的集合可能为Empty值
 */
fun AccessibilityNodeInfo.findNodeListById(id: String) : List<AccessibilityNodeInfo> = findAccessibilityNodeInfosByViewId(id)

/**
 * 扩展函数
 * 根据viewId查找第一个节点并返回
 * @return 返回的节点对象可能为Null值
 */
fun AccessibilityNodeInfo.findNodeById(id: String) : AccessibilityNodeInfo? {
    val dataList = findNodeListById(id)
    if (dataList.isNotEmpty()) {
        return dataList[0]
    }
    return null
}

/**
 * Accessibility 矩形绘制对象
 *
 * @author Amit
 * @date 2024/05/27
 */
data class AccessibilityRectNode(
    val className: String,
    val rect: Rect,
    val isVisibleToUser: Boolean = true,
    val parent: AccessibilityRectNode?,
    var totalChildCount: Int = 0,
    val childList: ArrayList<AccessibilityRectNode> = arrayListOf()
) {

    /** 获取当前节点是否为根节点 */
    fun isRootNode() : Boolean = (parent == null)

    /** 获取当前节点是否有子节点 */
    fun hasChildNode() : Boolean = (totalChildCount != 0)
}

/**
 * Accessibility Utils
 * 无障碍服务节点工具类
 *
 * @author Amit
 * @date 2024/05/28
 */
object AccessibilityUtils {

    /**
     * 将 AccessibilityNodeInfo 对象转换成 AccessibilityRectNode 对象
     *
     * @return
     *      null: 参数event无效，或转换失败；
     *    object: 转换成功的可绘制矩形节点；
     */
    fun makeRectNode(info: AccessibilityNodeInfo?): AccessibilityRectNode? = info?.run {
        // 1) 获得根节点：使用AccessibilityService.getRootInActiveNode时，此时参数info就是根节点
        var parent = this
        while (parent.parent != null) {
            parent = parent.parent
        }
        // 2) 递归创建可绘制节点
        return recursionMakeRectNode(parent, null)
    }

    /**
     * 根据参数递归创建 AccessibilityRectNode 对象
     *
     * @param node
     *      此对象为系统节点，通过AccessibilityService获取；
     * @param parent
     *      此对象为自定义可绘制节点，根节点时该参数值为null；
     * @return
     *      返回参数 node 下的全部可绘制节点
     */
    fun recursionMakeRectNode(node: AccessibilityNodeInfo, parent: AccessibilityRectNode?):
                                                    AccessibilityRectNode = node.run {
        // 节点组件的尺寸 [x - y, width - height]
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        // 创建可绘制节点
        val rectNode = AccessibilityRectNode(node.className.toString(), // 被绘制组件类型
                                             bounds,                    // 被绘制组件区域
                                             node.isVisibleToUser,      // 被绘制组件用户是否可见
                                             parent)                    // 被绘制组件的父组件
        for (idx in 0 until node.childCount) {
            // 递归：将所有字节点装入到 childNodeList 集合中
            node.getChild(idx)?.let {
                val child = recursionMakeRectNode(it, rectNode)
                if (child.isVisibleToUser) { // 用户可见
                    rectNode.childList.add(child)
                }
                child.totalChildCount += child.childList.size // 子节点下的全部节点个数
                rectNode.totalChildCount += child.totalChildCount // 记录全部子节点个数
            }
        }
        rectNode // 返回可绘制节点
    }

    /** 打印节点信息 */
    fun debugNode(debugTag: String, node: AccessibilityRectNode?) {
        node?.let {
            val space = StringBuilder().apply {
                var deep = 0
                var n = it.parent
                while (n != null) {
                    append("  ")
                    n = n.parent
                    deep++
                }
                insert(0, "$deep, ")
            }
            Log.i(debugTag, "-> ${ space }${ it.className }, ${ it.rect }, ${ it.totalChildCount }, ${ it.isVisibleToUser } ")

            for (idx in 0 until it.childList.size) {
                debugNode(debugTag, it.childList[idx])
            }
        }
    }

}
