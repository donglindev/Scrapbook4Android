package com.mb.scrapbook.lib.view.avt.engine

import com.mb.scrapbook.lib.view.avt.*
import org.json.JSONArray
import org.json.JSONException
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * 海豚抽象组件树引擎
 * 此引擎将自定义楼层JSON数据转换成一颗抽象组件树对象
 *
 * @author Amit
 * @date 2022/01/02
 */
class DolphinVTEngine() : ViewTreeEngine<String>() {

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

    /**
     * 通过此函数可将FLEX_WIDGET楼层转换成一颗抽象组件树对象
     * 解析海豚布局JSON数据，并采用尾插法生成抽象组件树对象；
     */
    override fun makeTree(source: String?): ViewTreeNode<Any?, ViewAttr>? = source?.let { s ->
        val deep = AtomicInteger(0)
        // 1) 创建根节点root对象，并创建lastNode尾节点对象；
        var attr = JsonAttr(s)
        var root = makeSelfNode(attr)
        var lastNode = root

        // 2) 创建flexWidget节点对象
        val flexWidget by attr
        flexWidget?.let {
            attr = JsonAttr(it)
            val l = lastNode
            val node = makeSelfNode(attr)
            lastNode = node
            l.child = node
            deep.incrementAndGet()
        }

        // 3) 递归创建viewGroups、imageViews和textViews对象
        val stack = Stack<JsonAttr>()
        stack.push(attr)
        do {
            attr = stack.pop()
            val siblingList = LinkedList<JsonAttr>()
            // 解析ViewGroup对象
            val viewGroups by attr
            viewGroups?.let {
                val groups = makeAttrArray(it)
                siblingList.addAll(groups)
                // 将ViewGroup元素添加到递归栈中
                // groups.forEach { stack.push(it) }
            }
            // 解析ImageView对象
            val imageViews by attr
            imageViews?.let { siblingList.addAll(makeAttrArray(it)) }
            // 解析TextView对象
            val textViews by attr
            textViews?.let { siblingList.addAll(makeAttrArray(it)) }

            // 将所有兄弟节点连接成当前深度链
            if (!siblingList.isEmpty()) {
                deep.incrementAndGet()
                // 根据index字段排序
                siblingList.sortBy { it.sortIndex }
                /**
                 * 使用尾插法创建兄弟节点链
                 * 1) 兄弟节点链第一个元素指向index最小的对象
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
            }

        } while (!stack.isEmpty())

        println(root)
        null // return result
    } ?: null


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
    private fun makeViewAttr(attr: JsonAttr): ViewAttr = attr?.let { json ->
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

}