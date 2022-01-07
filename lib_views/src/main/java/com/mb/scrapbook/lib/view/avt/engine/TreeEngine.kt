package com.mb.scrapbook.lib.view.avt.engine

/**
 * 树引擎
 * 此引擎将输入源转换成一颗树，它参考编译系统中抽象语法树的创建方式；
 *
 * @author Amit
 * @date 2022/01/01
 */
interface TreeEngine<in Source, out Tree> {

    /**
     * 创建一颗抽象组件树
     */
    fun makeTree(source: Source?) : Tree;
}