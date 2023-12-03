package com.mb.scrapbook.lib.view.avt.engine

import com.mb.scrapbook.lib.view.avt.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

/**
 * 海豚抽象组件树引擎
 * 引擎使用自定义楼层“布局数据”创建一颗抽象组件树对象
 *
 * @author Amit
 * @date 2022/01/02
 */
class DolphinVTEngine() : ViewTreeEngine<String>() {

    /**
     * 入口函数
     * 通过此函数可将FLEX_WIDGET楼层转换成一颗抽象组件树对象，此树用于描述自定义布局全貌；
     * 解析海豚布局JSON数据，并采用尾插法生成抽象组件树对象；
     * 1) root节点描述根楼层组件，支持楼层背景图/背景色设置；
     * 2) container节点描述flexWidget字段，支持圆角、外边距、内边距设置；
     * 3) 递归节点描述自定义楼层内部组件；
     */
    override fun makeTree(source: String?): ViewTreeNode<Any?, ViewAttr?>? = source?.let { s ->
        // 1) 创建根节点root对象，并创建lastNode尾节点对象；
        var attr = JsonAttr(s)
        var root = makeSelfNode(attr)
        var lastNode = root

        // 2) 创建flexWidget容器节点对象，并递归创建自定义楼层组件节点
        var container: ViewTreeNode<Any?, ViewAttr?>? = null
        val flexWidget by attr
        flexWidget?.let {
            attr = JsonAttr(it)
            val flexNode = makeSelfNode(attr)
            container = recursionTree(flexNode)
        }

        // 3) 将容器节点添加到抽象组件树上
        container?.let {
            val tmp = lastNode
            lastNode = it
            tmp.child = it
        }
        return root // return result
    } ?: null

    /**
     * 递归创建抽象组件树
     */
    private fun recursionTree(
        node: ViewTreeNode<Any?, ViewAttr?>
    ): ViewTreeNode<Any?, ViewAttr?>? = node.run {
        this.attrs?.let { a ->
            // 创建node节点下的兄弟链；
            val json = JSONObject(a.extends)
            var attr = JsonAttr(json.toString())
            val chainSibling = makeSibling(attr)
            // 递归遍历兄弟链节点，创建每一个兄弟节点的孩子节点，并将孩子添加到抽象组件树上；
            var n = chainSibling
            while (n != null) {
                n.attrs?.let { va ->
                    if (va is ViewGroupAttr) {
                        recursionTree(n!!)
                    }
                }
                n = n.sibling?.let { (it as ViewTreeNode<Any?, ViewAttr?>) } ?: null
            }
            // 将新创建的兄弟链连接到最后一个节点的孩子链上
            this.child = chainSibling
            return this
        }
        null
    }

    /**
     * 创建参数attr下的兄弟节点链
     */
    private fun makeSibling(attr: JsonAttr): ViewTreeNode<Any?, ViewAttr?>? {
        val viewGroups by attr
        val imageViews by attr
        val textViews by attr
        val siblingList = LinkedList<JsonAttr>()
        // 3.1.a) 创建ViewGroup节点数组
        viewGroups?.let {
            val tmp = makeAttrArray(it)
            siblingList.addAll(tmp)
        }
        // 3.1.b) 创建ImageView节点数组
        imageViews?.let {
            val tmp = makeAttrArray(it)
            siblingList.addAll(tmp)
        }
        // 3.1.c) 创建TextView节点数组
        textViews?.let {
            val tmp = makeAttrArray(it)
            siblingList.addAll(tmp)
        }
        // 3.1.d) 创建兄弟节点链
        if (!siblingList.isEmpty()) {
            // 根据index字段排序，排序后index值最小的元素做为链表中第一个元素；
            siblingList.sortBy { it.sortIndex }
            /*
             * 使用尾插法创建兄弟节点链
             */
            var first: ViewTreeNode<Any?, ViewAttr?>? = null
            var last: ViewTreeNode<Any?, ViewAttr?>? = null
            siblingList.forEach {
                val l = last
                val newNode = makeSelfNode(it)
                last = newNode
                if (l == null) {
                    first = last
                } else {
                    l.sibling = newNode
                }
            }
            return first
        }
        return null
    }

    /**
     * 根据节点属性(JsonAttr)创建抽象组件树节点(ViewTreeNode)对象，此对象不包括子节点和兄弟节点；
     * 使用属性委托方式创建抽象组件树各属性(extends Attr)对象；
     */
    private fun makeSelfNode(attr: JsonAttr) : ViewTreeNode<Any?, ViewAttr?> = ViewTreeNode(
        attrs = makeViewAttr(attr)
    )

    /**
     * 根据节点属性(JsonAttr)创建抽象组件树节点的属性(ViewAttr)对象
     */
    private fun makeViewAttr(attr: JsonAttr): ViewAttr = attr.let { json ->
        /**
         * 根据布局类型(layoutType)和楼层ID(floorId)两个属性确定是否是ViewGroupAttr对象
         */
        var floorId by json
        var layoutType by json

        val attrView =
            makeViewAttr(layoutType)?.let { vg -> vg } ?:
            makeViewAttr(floorId)?.let { vg -> vg } ?:
            ViewAttr()

        // 当前View的全部属性
        attrView.apply {
            // 扩展参数
            this.extends = json.toMap()
            // 组件尺寸属性
            this.size = Attr.Size().apply {
                val width by json
                width?.let { this.width = it.toInt() }
                val height by json
                height?.let { this.height = it.toInt() }
                val layoutWidth by json
                layoutWidth?.let { this.layoutWidth = it }
                val layoutHeight by json
                layoutHeight?.let { this.layoutHeight = it }
            }

            // 组件距屏幕绝对位置属性
            this.margin = Attr.Margin().apply {
                val marginLeft by json
                marginLeft?.let { this.left = it.toInt() }
                val marginTop by json
                marginTop?.let { this.top = it.toInt() }
                val marginRight by json
                marginRight?.let { this.right = it.toInt() }
                val marginBottom by json
                marginBottom?.let { this.bottom = it.toInt() }
            }

            // 组件内边距属性
            this.padding = Attr.Padding().apply {
                val paddingLeft by json
                paddingLeft?.let { this.left = it.toInt() }
                val paddingTop by json
                paddingTop?.let { this.top = it.toInt() }
                val paddingRight by json
                paddingRight?.let { this.right = it.toInt() }
                val paddingBottom by json
                paddingBottom?.let { this.bottom = it.toInt() }
            }

            // 组件内容方向属性
            this.gravity = Attr.Gravity().apply {
                val gravity by json
                gravity?.let { this.gravity = it }
            }

            // 组件背景属性
            this.background = Attr.Background().apply {
                val backgroundColor by json
                backgroundColor?.let { this.color = it }
                val backgroundImage by json
                backgroundImage?.let { this.image = it }
            }

            // 组件圆角属性
            this.corner = Attr.Corner().apply {
                val backgroundCorner by json
                backgroundCorner?.let {
                    val corner = it.toInt()
                    this.leftTop = corner
                    this.leftBottom = corner
                    this.rightTop = corner
                    this.rightBottom = corner
                }
                val topLeftRadius by json
                topLeftRadius?.let { this.leftTop = it.toInt() }
                val topRightRadius by json
                topRightRadius?.let { this.rightTop = it.toInt() }
                val bottomLeftRadius by json
                bottomLeftRadius?.let { this.leftBottom = it.toInt() }
                val bottomRightRadius by json
                bottomRightRadius?.let { this.rightBottom = it.toInt() }
            }
        }
    }

    /**
     * 创建ViewGroupAttr对象
     * 本函数位于makeViewAttr(attr: JsonAttr)函数中调用；
     * 本函数根据传入参数string创建ViewGroupAttr对象
     * 1) 当传入参数为Null，则返回Null对象；
     * 2) 当传入参数是orientation值，根据orientation创建对象；
     * 3) 当传入参数是floorId值，则返回垂直布局对象；
     */
    private fun makeViewAttr(string: String?): ViewAttr? = string?.let { content ->
        ViewGroupAttr().apply {
            this.orientation = Attr.Orientation().apply {
                when (content) {
                    Attr.STR_HORIZONTAL -> { this.orientation = Attr.HORIZONTAL }
                    Attr.STR_VERTICAL -> { this.orientation = Attr.VERTICAL }
                    else -> { this.orientation = Attr.VERTICAL }
                }
            }
        }
    } ?: null

    /**
     * 根据参数group创建抽象组件树节点属性集合
     * 返回值集合中只包含ViewGroup、ImageView和TextView的节点属性对象
     */
    private fun makeAttrArray(group: String): LinkedList<JsonAttr> = LinkedList<JsonAttr>().run {
        try {
            val array = JSONArray(group)
            for (idx in 0..array.length()) {
                val item = array[idx]
                add(JsonAttr(item.toString()))
            }
        } catch (e: JSONException) {
        } catch (e: Exception) {
        }
        this
    }

}