package com.mb.scrapbook.lib.base

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import com.mb.scrapbook.lib.base.common.AppManager

/**
 * Base Application
 * 存储全局Context和支持multi dex
 *
 * @author DongLin
 * @date 2021/07/27
 */
open class BaseApplication: MultiDexApplication() {

    companion object {
        // 相当于全局Context对象
        lateinit var instance: BaseApplication
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 注册ActivityLifecycleCallback监听
        registerActivityLifecycleCallbacks(ActivityLifecycleImpl())
    }

    private class ActivityLifecycleImpl: ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity,
                                       savedInstanceState: Bundle?) {
            AppManager.instance.addActivity(activity)
        }

        override fun onActivityDestroyed(activity: Activity) {
            AppManager.instance.delActivity(activity)
        }

        override fun onActivityStarted(activity: Activity) { }

        override fun onActivityResumed(activity: Activity) { }

        override fun onActivityPaused(activity: Activity) { }

        override fun onActivityStopped(activity: Activity) { }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) { }
    }
}