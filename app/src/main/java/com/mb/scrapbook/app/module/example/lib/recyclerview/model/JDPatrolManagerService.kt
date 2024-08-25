package com.mb.scrapbook.app.module.example.lib.recyclerview.model

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.os.bundleOf
import com.blankj.utilcode.util.ScreenUtils
import com.mb.scrapbook.lib.base.model.click
import com.mb.scrapbook.lib.base.model.findNodeById
import com.mb.scrapbook.lib.base.model.findNodeByText
import com.mb.scrapbook.lib.base.model.input
import com.mb.scrapbook.lib.base.model.scrollForward
import com.mb.scrapbook.lib.base.mvvm.view.ScreenshotActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedList

/**
 * 京东巡检管理服务
 *
 * @author donglin6
 * @date 2024/06/13
 */
class JDPatrolManagerService(private val patrolImpl: JDPagePatrolListener?) {

    /**
     * 京东巡检监听器
     */
    interface JDPagePatrolListener {
    }

    /**
     * 京东页面巡检
     */
    sealed class PagePatrol(private val listenerImpl: JDPagePatrolListener?) {
        /** 巡检方法 */
        abstract fun run(context: Context, event: AccessibilityNodeInfo?)

        /**
         * 默认巡检
         */
        class EmptyPatrol(listenerImpl: JDPagePatrolListener?) : PagePatrol(listenerImpl) {
            override fun run(context: Context,
                             event: AccessibilityNodeInfo?
            ) {
                Log.d(TAG, ":-> EmptyPatrol")
            }
        }

        /**
         * 京东开屏页巡检
         */
        class MainActivityPatrol(listenerImpl: JDPagePatrolListener?) : PagePatrol(listenerImpl) {
            override fun run(context: Context,
                             event: AccessibilityNodeInfo?
            ) {
                Log.d(TAG, ":-> 开始巡检京东开屏页...")
                // 查找“同意”按钮，并点击进入京东首页
                event?.let {
                    it.findNodeByText("同意")?.let { node ->
                        Log.d(TAG, ":-> 点击 同意 进入京东应用首页...")
                        node.click()
                    }
                }
            }
        }

        /**
         * 京东首页巡检
         */
        class MainFrameActivityPatrol(listenerImpl: JDPagePatrolListener?) : PagePatrol(listenerImpl) {
            override fun run(context: Context,
                             event: AccessibilityNodeInfo?
            ) {
                Log.d(TAG, ":-> 开始巡检京东首页...")
                event?.let {
                    // 查找页面上“立即登录”按钮并点击
                    it.findNodeByText("立即登录")?.apply {
                        // 由于“立即登录”文本是不可点击组件，所以需要查找到该组件的父组件并点击
                        val stack = arrayListOf(this)
                        while (stack.isNotEmpty()) {
                            stack.removeLast().let { node ->
                                if (node.isClickable) { // 此节点可点击
                                    stack.clear() // 清空栈
                                    Log.d(TAG, ":-> 点击 立即登录 进入京东登录页...")
                                    node.click() // 点击立即登录
                                } else { // 此节点不可点击
                                    node.parent?.let { parent -> stack.add(parent) }
                                }
                            }
                        }
                    }
                    // 查找“京东国际”按钮并点击
                    it.findNodeByText("京东国际")?.apply {
                        val stack = arrayListOf(this)
                        while (stack.isNotEmpty()) {
                            stack.removeLast().let { node ->
                                if (node.isClickable) {
                                    stack.clear()
                                    Log.d(TAG, ":-> 点击 京东国际 进入京东国际频道页...")
                                    node.click()
                                } else {
                                    node.parent?.let { parent -> stack.add(parent) }
                                }
                            }
                        }
                    }
                }

            }
        }

        /**
         * 手机号登录页巡检
         */
        class MobileLoginActivityPatrol(listenerImpl: JDPagePatrolListener?) : PagePatrol(listenerImpl) {
            override fun run(context: Context,
                             event: AccessibilityNodeInfo?
            ) {
                Log.d(TAG, ":-> MobileLoginActivityPatrol")
            }
        }

        /**
         * 账号登录页巡检
         */
        class LoginActivityPatrol(listenerImpl: JDPagePatrolListener?) : PagePatrol(listenerImpl) {

            companion object {
                private const val ACCOUNT = "三单008a-_"
                private const val PASSWORD = "a54a71y4"
            }

            override fun run(context: Context,
                             event: AccessibilityNodeInfo?
            ) {
                Log.d(TAG, ":-> 开始执行登录逻辑...")
                event?.let {
                    // 查找页面上“账号密码登录”按钮并点击
                    it.findNodeByText("账号密码登录")?.let { node ->
                        if (node.isClickable) {
                            Log.d(TAG, ":-> 切换账号密码登录...")
                            node.click()
                        }
                    }
                    it.findNodeByText("短信验证码登录")?.let { node ->
                        it.findNodeById("com.jd.lib.login.feature:id/ei")?.input(ACCOUNT) // 账号
                        it.findNodeById("com.jd.lib.login.feature:id/ek")?.input(PASSWORD) // 密码
                        it.findNodeById("com.jd.lib.login.feature:id/jm")?.let { agree ->
                            Log.d(TAG, "${ agree.className }, ${ agree.isChecked }")
                            val stack = arrayListOf(agree)
                            while (stack.isNotEmpty()) {
                                stack.removeLast().let { node ->
                                    if (node.isClickable) {
                                        stack.clear()
                                        Log.d(TAG, ":-> 勾选政策同意...")
                                        node.click()
                                    } else {
                                        node.parent?.let { parent -> stack.add(parent) }
                                    }
                                }
                            }
                        }
                        it.findNodeById("com.jd.lib.login.feature:id/m")?.let { submit ->
                            Log.d(TAG, ":-> 点击 登录 再次进入京东首页...")
                             submit.click()
                        }
                    }
                }
            }
        }

        /**
         * 海豚频道页巡检
         */
        class DolphinActivityPatrol(listenerImpl: JDPagePatrolListener?) : PagePatrol(listenerImpl) {

            /** 屏幕的宽 */
            private val screenWidth by lazy { ScreenUtils.getScreenWidth() }
            /** 屏幕的高 */
            private val screenHeight by lazy { ScreenUtils.getScreenHeight() }
            /** 日期格式化对象 */
            private val dayFormatter by lazy { SimpleDateFormat("yyyyMMdd") }
            /** 事件格式化对象 */
            private val timeFormatter by lazy { SimpleDateFormat("HHmmss") }

            /** 巡检栈 */
            private val patrolStack = LinkedList<String>()

            /**
             * 当前被巡检的楼层索引
             */
            private var currentPatrolIndex = -1

            /** 伴生对象 */
            companion object {
                /** 巡检截图前缀 patrol_yyyyMMdd */
                private const val DIR_PATROL_NAME = "jd_patrol"
                /** 顶部导航组件名称 */
                private const val ID_HEAD_VIEW = "com.jd.lib.dolphin.feature:id/dolphin_sliding_head"
                /** 底部导航组件名称 */
                private const val ID_BOTTOM_VIEW = "com.jd.lib.dolphin.feature:id/dolphin_bottom"
                /** 中间内容组件名称 */
                private const val ID_CONTENT_VIEW = "com.jd.lib.dolphin.feature:id/pull_to_refresh_wrapper"
                /** 截屏Activity */
                private const val SCREENSHOT_ACTIVITY = "com.mb.scrapbook.lib.base.mvvm.view.ScreenshotActivity"

                /** 节点露出百分比 */
                private const val NODE_SHOW_PERCENT = 0.55f

                /** 巡检栈：截屏 */
                const val STACK_SCREENSHOT = "stackScreenshot"
                /** 巡检栈：容器翻页 */
                const val STACK_CONTAINER_SCROLL = "stackContainerScroll"
            }

            /** 在巡检栈中查找手动触发的元素 */
            fun inPatrolStack(stackProcess: String) = patrolStack.any { it == stackProcess }

            /** 创建截屏Intent对象 */
            private fun makeScreenshotIntent(context: Context,
                                             targetScreenshot: String,
                                             rectScreenshot: Rect
            ) = Intent().apply {
                component = ComponentName(context.packageName, SCREENSHOT_ACTIVITY)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtras(bundleOf(
                    Pair(ScreenshotActivity.EXTRA_PATH_SCREENSHOT, targetScreenshot), // path
                    Pair(ScreenshotActivity.EXTRA_RECT_SCREENSHOT, rectScreenshot) // rect
                ))
            }

            /**
             * 楼层可见度校验
             * TODO: 楼层必须在屏幕上露出70%，才对该楼层做巡检处理；
             */
            private fun nodeIsVisible(headViewHeight: Int,
                                      bottomViewHeight: Int,
                                      node: AccessibilityNodeInfo,
            ) : Boolean {
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                // 节点水平方向完全未露出
                if (bounds.left > screenWidth || bounds.right < 0) {
                    return false // 组件不在屏幕中
                }
                // 节点垂直方向完全未露出
                else if (bounds.top >= (screenHeight - bottomViewHeight) || bounds.bottom <= headViewHeight) {
                    return false // 组件不在屏幕中
                }
                // 节点水平方向露出部分
                if (bounds.left < 0 && bounds.left + bounds.width() < bounds.width() * NODE_SHOW_PERCENT) {
                    return false // 节点未露出整体宽的 NODE_SHOW_PERCENT 百分比
                } else if (screenWidth - bounds.left < bounds.width() * NODE_SHOW_PERCENT ) {
                    return false // 节点未露出整体宽的 NODE_SHOW_PERCENT 百分比
                }
                // 节点垂直方向露出部分
                if (bounds.top < headViewHeight && bounds.bottom - headViewHeight < bounds.height() * NODE_SHOW_PERCENT) {
                    return false // 节点未露出整体高的 NODE_SHOW_PERCENT 百分比
                } else if (screenHeight - bottomViewHeight - bounds.top < bounds.height() * NODE_SHOW_PERCENT) {
                    return false // 节点未露出整体高的 NODE_SHOW_PERCENT 百分比
                }
                return true
            }

            /** 获得海豚频道页顶部导航高度 */
            private fun getHeadViewHeight(event: AccessibilityNodeInfo?) : Int = event?.let { node ->
                node.findNodeById(ID_HEAD_VIEW)?.let { view ->
                    val rectHead = Rect()
                    view.getBoundsInScreen(rectHead)
                    rectHead.height() // 顶部导航高度
                } ?: 0
            } ?: 0 // 参数无效时返回0

            /** 获得海豚频道页底部导航高度 */
            private fun getBottomViewHeight(event: AccessibilityNodeInfo?) : Int = event?.let { node ->
                node.findNodeById(ID_BOTTOM_VIEW)?.let { view ->
                    val rectBottom = Rect()
                    view.getBoundsInScreen(rectBottom)
                    rectBottom.height() // 底部导航高度
                } ?: 0
            } ?: 0

            /** 应该继续巡检 */
            private fun continuePatrolIfNecessary() = patrolStack.isEmpty()

            /**
             * 开始巡检海豚频道页
             */
            override fun run(context: Context, event: AccessibilityNodeInfo?) {
                // 巡检栈为空时：说明首次进入频道页，直接运行巡检；
                if (patrolStack.isEmpty()) {
                    runPatrol(context, event)
                    return // 忽略：对栈的其他操作
                }

                // 巡检栈中只有一个操作元素，移除元素后通过校验“上一次”操作是什么，再做最终决定处理；
                val areSingleElement = (patrolStack.size == 1)
                if (areSingleElement) {
                    patrolStack.pollLast().apply {
                        when (this) { // 此时巡检栈为空
                            // 上一次操作是截屏：继续巡检
                            STACK_SCREENSHOT -> runPatrol(context, event)
                            // 上一次操作是滑动：继续巡检
                            STACK_CONTAINER_SCROLL -> runPatrol(context, event)
                        }
                    }
                }
            }

            /** 执行巡检 */
            private fun runPatrol(context: Context,
                                  event: AccessibilityNodeInfo?
            ) {
                event?.let { node -> node.findNodeById(ID_CONTENT_VIEW)?.let { content ->
                    val headHeight = getHeadViewHeight(event) // 顶部导航栏高度
                    val bottomHeight = getBottomViewHeight(event) // 底部导航栏高度

                    content.getChild(0)?.let { container ->
                        // 1、巡检屏幕上楼层（可见的楼层）
                        for (idx in 0 until container.childCount) {
                            // TODO：关键代码（重点说明），是否继续巡检
                            if (continuePatrolIfNecessary()) {
                                container.getChild(idx)?.let { floor ->
                                    // TODO：关键代码（重点说明），只有当前楼层索引大于上一次保存的楼层索引才巡检
                                    if (idx > currentPatrolIndex) {
                                        floorOfPatrol(context, headHeight, bottomHeight, idx, floor) // 楼层巡检
                                    }
                                }
                            }
                        } // for END

                        // 2、滚动屏幕继续巡检
                        if (continuePatrolIfNecessary()) {
                            container.getChild(container.childCount - 1)?.let { floor ->
                                /*
                                 * 校验屏幕中最后一个楼层
                                 * TODO：如何判断最后一个楼层是feeds流楼层？
                                 */
                                val rect = Rect()
                                floor.getBoundsInScreen(rect)

                                if (rect.top != headHeight) {
                                    // 容器滑动后清空巡检索引
                                    currentPatrolIndex = -1
                                    startScroll(container)
                                }
                            }
                        }

                    } // container END
                } // content END
                } // node END
            }


            /**
             * 巡检容器中每一个楼层
             *
             * @param index 楼层在容器中的索引位置
             * @param floor 楼层节点
             * @param headHeight 顶部导航栏高度
             * @param bottomHeight 底部导航栏高度
             */
            private fun floorOfPatrol(context: Context, headHeight: Int, bottomHeight: Int,
                                      index: Int, floor: AccessibilityNodeInfo) {

                currentPatrolIndex = index // 更新巡检楼层索引
                // if (index == 1) return // TODO: DEBUG
                // 楼层截图保存目录：/xxx/yyyyMMdd/HHmmss_index
                val dirTarget = mkdirPatrolOfFloorByTime(context, index)

                /*
                 * 楼层在屏幕中可见区域超过55%
                 *
                 */
                if (nodeIsVisible(headHeight, bottomHeight, floor)) {
                    val pathRoot: String = captureRootNode(context, floor, dirTarget)
                    Log.d(TAG, "floor in screen: $pathRoot")

                    LinkedList<AccessibilityNodeInfo>().let { stack ->
                        val clickNodeList = arrayListOf<AccessibilityNodeInfo>()
                        val scrollNodeList = arrayListOf<AccessibilityNodeInfo>()
                        stack.push(floor)
                        while (!stack.isEmpty()) {
                            stack.pollLast()?.let { node ->
                                if (nodeIsVisible(headHeight, bottomHeight, node)) {
                                    if (node.isClickable) {
                                        clickNodeList.add(node)
                                    } else if (node.isScrollable) {
                                        scrollNodeList.add(node)
                                    }
                                }

                                for (idx in 0 until node.childCount) {
                                    node.getChild(idx)?.let { stack.push(it) }
                                }
                            }
                        }

                        Log.d(TAG, ":-> floor_$index: clicked: ${ clickNodeList.size }, scrolled: ${ scrollNodeList.size } ")
                    }

                }
                /* 楼层在屏幕中不可见时 */
                else { }

            } // floorOfPatrol END

            /** 楼层根节点截图 */
            private fun captureRootNode(context: Context,
                                        floor: AccessibilityNodeInfo,
                                        dirBase: File
            ) = File(dirBase, "0.png").run {
                val path = this
                val rect = Rect()
                floor.getBoundsInScreen(rect)

                val capture = makeScreenshotIntent(context, path.absolutePath, rect)
                startCapture(context, capture)

                path.absolutePath
            }

            /** 节点滑动 */
            private fun startScroll(node: AccessibilityNodeInfo) {
                patrolStack.add(STACK_CONTAINER_SCROLL)
                node.scrollForward()
            }

            /** 通过ScreenshotActivity截屏(楼层区域) */
            private fun startCapture(context: Context, intent: Intent) {
                patrolStack.add(STACK_SCREENSHOT) // 截屏操作进巡检栈
                context.startActivity(intent) // 截屏
            }

            /** 创建楼层巡检目录 */
            private fun mkdirPatrolOfFloorByTime(context: Context, index: Int) = timeFormatter.run {
                val dirBase = mkdirPatrolOfDay(context) // /xxx/yyyyMMdd
                val targetName = "${ format(Date(System.currentTimeMillis())) }_$index"
                val target = File(dirBase, targetName) // /xxx/yyyyMMdd/HHmmss_index
                if (!target.exists()) {
                    target.mkdirs()
                }
                target
            }

            /** 创建每日巡检目录 */
            private fun mkdirPatrolOfDay(context: Context) = dayFormatter.run {
                val dirBase = context.getExternalFilesDir(null)
                val dirPatrol = File(dirBase, DIR_PATROL_NAME)
                val target = File(dirPatrol, format(Date(System.currentTimeMillis())))
                if (!target.exists()) {
                    target.mkdirs()
                }
                // /storage/emulate/0/Android/data/com.x.x/files/jd_patrol/yyyyMMdd
                target
            }

        } // END DolphinActivity
    }

    companion object {
        // 日志
        private const val TAG = "donglin"
        /** 京东开屏页 */
        private const val PAGE_MAIN_ACTIVITY = "com.jingdong.app.mall.main.MainActivity"
        /** 京东首页*/
        private const val PAGE_MAIN_FRAME_ACTIVITY = "com.jingdong.app.mall.MainFrameActivity"
        /** 手机号登录页 */
        private const val PAGE_CHINA_MOBILE_LOGIN_ACTIVITY = "com.jd.lib.login.ChinaMobileLoginActivity"
        /** 账号登录页 */
        private const val PAGE_LOGIN_ACTIVITY = "com.jd.lib.login.LoginActivity"
        /** 海豚频道页 */
        private const val PAGE_DOLPHIN_ACTIVITY = "com.jd.lib.dolphin.view.activity.DolphinActivity"
    }

    /** 京东开屏页巡检对象 */
    private val patrolMainActivity: PagePatrol.MainActivityPatrol by lazy {
                    PagePatrol.MainActivityPatrol(patrolImpl)
    }
    /** 京东首页*/
    private val patrolMainFrameActivity: PagePatrol.MainFrameActivityPatrol by lazy {
                    PagePatrol.MainFrameActivityPatrol(patrolImpl)
    }
    /** 手机号登录页 */
    private val patrolMobileLoginActivity: PagePatrol.MobileLoginActivityPatrol by lazy {
                    PagePatrol.MobileLoginActivityPatrol(patrolImpl)
    }
    /** 账号登录页 */
    private val patrolLoginActivity: PagePatrol.LoginActivityPatrol by lazy {
                    PagePatrol.LoginActivityPatrol(patrolImpl)
    }
    /** 海豚频道页 */
    private val patrolDolphinActivity: PagePatrol.DolphinActivityPatrol by lazy {
                    PagePatrol.DolphinActivityPatrol(patrolImpl)
    }
    /** 空巡检 */
    private val patrolEmptyActivity: PagePatrol.EmptyPatrol by lazy {
                    PagePatrol.EmptyPatrol(patrolImpl)
    }

    /** 根据页面创建巡检页对象 */
    fun makePagePatrol(page: String) = when (page) {
        // 京东开屏页
        PAGE_MAIN_ACTIVITY -> patrolMainActivity
        // 京东首页
        PAGE_MAIN_FRAME_ACTIVITY -> patrolMainFrameActivity
        // 手机号登录页
        PAGE_CHINA_MOBILE_LOGIN_ACTIVITY -> patrolMobileLoginActivity
        // 账号登录页
        PAGE_LOGIN_ACTIVITY -> patrolLoginActivity
        // 海豚频道页
        PAGE_DOLPHIN_ACTIVITY -> patrolDolphinActivity
        // 空巡检
        else -> patrolEmptyActivity
    }

    /** 京东开屏页 */
    fun isMainActivity(page: String) = (PAGE_MAIN_ACTIVITY == page)

    /** 京东首页 */
    fun isMainFrameActivity(page: String) = (PAGE_MAIN_FRAME_ACTIVITY == page)

    /** 手机号登录页 */
    fun isMobileLoginActivity(page: String) = (PAGE_CHINA_MOBILE_LOGIN_ACTIVITY == page)

    /** 账号登录 */
    fun isLoginActivity(page: String) = (PAGE_LOGIN_ACTIVITY == page)

    /** 海豚频道页 */
    fun isDolphinActivity(page: String) = (PAGE_DOLPHIN_ACTIVITY == page)
}