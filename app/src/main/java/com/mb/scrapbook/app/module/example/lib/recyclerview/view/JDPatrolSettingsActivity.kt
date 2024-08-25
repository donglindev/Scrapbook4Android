package com.mb.scrapbook.app.module.example.lib.recyclerview.view

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.ColorUtils

/**
 * JD巡检设置页
 *
 * @author donglin6
 * @date 2024/06/04
 */
class JDPatrolSettingsActivity: AppCompatActivity() {

    /** 生命周期 */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.mb.scrapbook.lib.view.R.layout.activity_fragment_container)
        BarUtils.setStatusBarColor(this, ColorUtils.string2Int("#F2F1F8"))
        BarUtils.setStatusBarLightMode(this, true)
        // 创建JDAccessibilityFragment对象并显示
        supportFragmentManager.apply {
            val transaction = beginTransaction()
            transaction.add(com.mb.scrapbook.lib.view.R.id.container, JDAccessibilityFragment())
            transaction.commit()
        }
        // 设置容器沉浸式模式
        findViewById<View>(com.mb.scrapbook.lib.view.R.id.container)?.apply {
            setPadding(
                paddingLeft, (paddingTop + BarUtils.getStatusBarHeight()), paddingRight, paddingBottom
            )
        }
    }
}