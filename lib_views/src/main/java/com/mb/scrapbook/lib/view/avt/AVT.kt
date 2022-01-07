package com.mb.scrapbook.lib.view.avt

import org.json.JSONObject
import kotlin.reflect.KProperty

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
    val size: Attr.Size = Attr.Size(),
    // 外边距
    val margin: Attr.Margin = Attr.Margin(),
    // 内边距
    val padding: Attr.Padding = Attr.Padding(),
    // 内容对齐
    val gravity: Attr.Gravity = Attr.Gravity(),
    // 背景样式
    val background: Attr.Background = Attr.Background(),
    // 圆角角度
    val corner: Attr.Corner = Attr.Corner(),
    // 扩展属性
    val extends: Map<String, Any?> = HashMap()
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
    val position: Attr.Position? = null,
    // 方向
    val orientation: Attr.Orientation? = null
) : ViewAttr()

/**
 * 树节点
 * 此节点包含数据域和关系域两部分组成
 *
 * @author Amit
 * @date 2021/12/26
 */
open class TreeNode<Data>(
    val data: Data? = null,
    val child: TreeNode<Data>? = null,
    val sibling: TreeNode<Data>? = null
)

/**
 * 组件树节点
 * 此节点继承自TreeNode<Data>类，并扩展了属性域部分
 *
 * @author Amit
 * @date 2021/12/26
 */
class ViewTreeNode<Data, Attr>(
    val attrs: Attr? = null,
    data: Data? = null,
    child: TreeNode<Data>? = null,
    sibling: TreeNode<Data>? = null
) : TreeNode<Data>(data, child, sibling)

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

    /** 属性委托中必须定义的getValue方法 */
    operator fun getValue(nothing: Nothing?, property: KProperty<*>): String? {
        val result = attrs[property.name];
        return result?.let { result.toString() } ?: null
    }

    /** 属性委托中必须定义的setValue方法 */
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
        attrs.put(property.name, value)
    }
}
