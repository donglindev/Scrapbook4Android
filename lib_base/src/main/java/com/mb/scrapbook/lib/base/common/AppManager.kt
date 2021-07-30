package com.mb.scrapbook.lib.base.common

import android.app.Activity
import java.util.*

/**
 * 应用自身Activity管理类
 * 维护着一个应用中Activity栈
 *
 * @author DongLin
 * @date 2021/07/30
 */
class AppManager private constructor(private val stackActivity: Stack<Activity>) {

    companion object {
        val instance: AppManager by lazy { AppManager(Stack()) }
    }

    /**
     * 向栈中添加Activity对象
     */
    fun addActivity(activity: Activity?) = activity?.let { stackActivity.push(activity) }


    /**
     * 从栈中删除Activity对象
     */
    fun delActivity(activity: Activity?) = activity?.let {
        if (it == stackActivity.peek()) {
            stackActivity.pop()
        } else {
            stackActivity.remove(it)
        }
    }


    /**
     * 销毁全部已存在的Activity
     */
    fun finishAllActivity() {
        if (!stackActivity.isNullOrEmpty()) {
            for (activity in stackActivity) {
                activity.finish()
            }
            stackActivity.clear()
        }
    }

}