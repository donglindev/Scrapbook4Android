package com.mb.scrapbook.lib.base.utils

import java.lang.reflect.ParameterizedType

/**
 * 通用工具类
 *
 * @author DongLin
 * @date 2021/07/28
 */
object Util {

    /**
     * 通过反射获取类定义的泛型T对应的Class对象
     * 此方法是Activity类ViewModel对象的通用创建过程；
     */
    fun <T> getClass(t: Any): Class<T> {
        return (t.javaClass.genericSuperclass as ParameterizedType)
                        .actualTypeArguments[0] as Class<T>
    }
}