package com.mb.scrapbook.lib.base

import android.content.Context
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication

/**
 * Base Application
 * 存储全局Context和支持multi dex
 *
 * @author DongLin
 * @date 2021/07/27
 */
open class BaseApplication: MultiDexApplication() {

    companion object {
        lateinit var instance: BaseApplication
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this;
    }
}