package com.mb.scrapbook.lib.view.avt.engine

import com.mb.scrapbook.lib.view.avt.ViewAttr
import com.mb.scrapbook.lib.view.avt.ViewTreeNode

/**
 * 抽象组件树引擎
 * 此引擎将输入源转换成一颗抽象组件树，它参考编译系统中抽象语法树的创建方式；
 *
 * @author Amit
 * @date 2022/01/01
 */
abstract class ViewTreeEngine<in Source>() : TreeEngine<Source, ViewTreeNode<Any?, ViewAttr>?>