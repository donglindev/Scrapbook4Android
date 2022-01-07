package com.mb.scrapbook.lib.view.avt.engine

import android.text.TextUtils
import android.util.Log
import com.google.gson.Gson
import com.mb.scrapbook.lib.view.avt.JsonAttr
import com.mb.scrapbook.lib.view.avt.ViewAttr
import com.mb.scrapbook.lib.view.avt.ViewTreeNode
import org.json.JSONObject

/**
 * 海豚抽象组件树引擎
 * 此引擎将自定义楼层JSON数据转换成一颗抽象组件树对象
 *
 * @author Amit
 * @date 2022/01/02
 */
class DolphinVTEngine() : ViewTreeEngine<String>() {

    /**
     * 通过此函数可将FLEX_WIDGET楼层转换成一颗抽象组件树对象
     */
    override fun makeTree(source: String?): ViewTreeNode<Any?, ViewAttr>? = source?.let { s ->
        // 委托对象：此对象是自定义楼层的根，也是JSON的根对象
        val attrRoot: JsonAttr = JsonAttr(source)

        val flexWidget by attrRoot
        Log.d("donglin", "+++ flexWidget: $flexWidget")
        null // return result
    } ?: null
}