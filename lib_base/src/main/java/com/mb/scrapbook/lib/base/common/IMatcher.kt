package com.mb.scrapbook.lib.base.common

/**
 * IMatcher
 * 匹配接口：实现匹配逻辑
 *
 * @author Amit
 * @date 2021/10/31
 */
interface IMatcher<T> {

    /**
     * 匹配逻辑：根据数据实现匹配逻辑，必须返回成功或失败；
     */
    fun match(data: T?): Boolean
}