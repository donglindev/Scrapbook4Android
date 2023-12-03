package com.mb.scrapbook.lib.view.avt

import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import java.lang.StringBuilder
import kotlin.collections.HashMap
import kotlin.reflect.KProperty

const val TAG_AVT = "AVT"

/**
 * 抽象组件树顶层类
 * 此文件中声明抽象组件树需要的相关基础数据定义
 *
 * @author Amit
 * @date 2021/12/26
 */

/**
 * 组件树节点属性
 * 此类用于描述一个组件节点的样式，包括：大小、外边距、内边距、方向等
 *
 * @author Amit
 * @date 2022/01/01
 */
open class ViewAttr(
    // 尺寸
    var size: Attr.Size = Attr.Size(),
    // 外边距
    var margin: Attr.Margin = Attr.Margin(),
    // 内边距
    var padding: Attr.Padding = Attr.Padding(),
    // 内容对齐
    var gravity: Attr.Gravity = Attr.Gravity(),
    // 背景样式
    var background: Attr.Background = Attr.Background(),
    // 圆角角度
    var corner: Attr.Corner = Attr.Corner(),
    // 扩展属性
    var extends: Map<String, Any?> = HashMap()
)

/**
 * 组件树节点属性
 * 此类用于描述一个组节点的样式，组节点比组件节点多了绝对位置和方向属性
 * 1) 绝对位置属性：浮层组件必须位于组节点中，并且绝对位置属性不可为空
 * 2) 当方向属性值为null时，此组表示FrameLayout组件，否则表示带方向的LinearLayout组件
 *
 * @author Amit
 * @date 2022/01/01
 */
open class ViewGroupAttr(
    // 相对屏幕绝对位置
    var position: Attr.Position? = null,
    // 方向
    var orientation: Attr.Orientation? = null
) : ViewAttr()

/**
 * 树节点
 * 此节点包含数据域和关系域两部分组成
 *
 * @author Amit
 * @date 2021/12/26
 */
open class TreeNode<Data>(
    var data: Data? = null,
    var child: TreeNode<Data>? = null,
    var sibling: TreeNode<Data>? = null
) {
    companion object {
        const val space = "  "
    }
}

/**
 * 组件树节点
 * 此节点继承自TreeNode<Data>类，并扩展了属性域部分
 *
 * @author Amit
 * @date 2021/12/26
 */
class ViewTreeNode<Data, Attr>(
    var attrs: Attr? = null,
    data: Data? = null,
    child: ViewTreeNode<Data, Attr>? = null,
    sibling: ViewTreeNode<Data, Attr>? = null
) : TreeNode<Data>(data, child, sibling) {

    /**
     * 前序打印抽象组件树
     */
    private fun preorder(deep: Int = 0, node: ViewTreeNode<Data, Attr>?) {
        node?.let { n ->
            val buff = StringBuilder().apply {
                (0..deep).forEach { _ -> append(space) }
                append("ViewTreeNode { ")
                append(n.attrs?.run { (this as ViewAttr).size } ?: null)
                append(" }")
            }
            Log.d(TAG_AVT, buff.toString())

            node.child?.let { c -> preorder((deep + 1), c as ViewTreeNode<Data, Attr>) }
            node.sibling?.let { s -> preorder(deep, s as ViewTreeNode<Data, Attr>) }
        }
    }

    /**
     * 输出树状结构
     */
    override fun toString(): String = "=============================".also { preorder(node = this) }
}

/**
 * JSON属性委托类
 * 此类将JSON字符串转换成JSONObject对象后，通过JSON文本中key作为property.name属性名，
 * 实现从JSONObject对象中获取key对应值的委托模式；
 *
 * @author Amit
 * @date 2022/01/08
 */
class JsonAttr(private val json: String) {

    /** JSONObject被委托对象 */
    private val attrs: JSONObject by lazy { JSONObject(json) }

    /** 排序字段，使用JSON中index值作为排序字段 */
    val sortIndex: Int by lazy {
        val index by this
        index?.let { it.toInt() } ?: 0
    }

    /**
     * 将JSON对象转换成Java中Map对象
     */
    fun toMap(): Map<String, Any?> = HashMap<String, Any?>().apply {
        val copied = JSONObject(json)
        val it = copied.keys()
        while (it.hasNext()) {
            it.next()?.let { k ->
                put(k, copied[k])
            }
        }
    }

    /** 属性委托中必须定义的getValue方法 */
    operator fun getValue(nothing: Nothing?, property: KProperty<*>): String? {
        try {
            val result = attrs[property.name]
            return result?.let { result.toString() } ?: null
        } catch (e: JSONException) {
        } catch (e: Exception) {
        }
        return null
    }

    /** 属性委托中必须定义的setValue方法 */
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
        try {
            attrs.put(property.name, value)
        } catch (e: Exception) {
        }
    }
}