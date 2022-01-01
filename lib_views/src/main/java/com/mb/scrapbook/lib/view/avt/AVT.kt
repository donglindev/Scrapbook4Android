package com.mb.scrapbook.lib.view.avt

/**
 * 抽象组件树顶层类
 * 此文件中声明抽象组件树需要的相关基础数据定义
 *
 * @author Amit
 * @date 2021/12/26
 */

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
