package com.mb.scrapbook.app.services

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.blankj.utilcode.util.ScreenUtils
import com.mb.scrapbook.app.module.example.lib.recyclerview.model.JDPatrolSettingsUtils
import com.mb.scrapbook.app.module.example.lib.recyclerview.repository.JDPatrolSettingsRepository.Companion.TOGGLE_ON
import com.mb.scrapbook.app.module.example.lib.recyclerview.repository.JDPatrolSettingsRepository.Companion.TYPE_SETTINGS
import com.mb.scrapbook.app.module.example.lib.recyclerview.repository.JDPatrolSettingsRepository.Companion.TYPE_SETTINGS_PRINT_ACCESSIBILITY
import com.mb.scrapbook.app.module.example.lib.recyclerview.view.JDPatrolDrawPanelView
import java.lang.Exception

/**
 * JD Accessibility Service
 * JD辅助服务-自动巡检（独立进程：jd_patrol）
 */
class JDAccessibilityService : AccessibilityService() {

    /** 巡检服务监控器 */
    private lateinit var mPatrolWatcher: PatrolWatcher

    /** JD巡检边界绘制组件 */
    private var viewJDPatrolDrawPanel: View? = null

    companion object {
        // 日志
        private const val TAG = "donglin"
        // 调试：打印Accessibility事件
        private const val DEBUG_ACCESSIBILITY = false

        /** 巡检监控器线程名称: JD AccessibilityService Watcher Name */
        const val WATCHER_THREAD_NAME = "JD-AS-W-N"
        /** 巡检服务心跳包间隔时间：如果15秒都未收到心跳，说明JD巡检服务已经出现问题，需要手动重启 */
        private const val PATROL_SERVICE_HEARTBEAT_INTERVAL = (1000L * 15)
        /** 巡检监控器心跳包 */
        const val MSG_PATROL_SERVICE_HEARTBEAT = 0xDD00
    }

    /** JD巡检监控器 */
    private inner class PatrolWatcher(looper: Looper) : Handler(looper) {
        /** 处理消息 */
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                // 特殊逻辑处理：巡检服务心跳包
                MSG_PATROL_SERVICE_HEARTBEAT -> {
                    Log.i(TAG, "!!! handle Patrol Service HeartBeat !!!")
                }
            }
        }

        /** 发送巡检服务心跳包 */
        /*package*/ fun sendPatrolServiceHeartBeat() {
            /*
             * 发送巡检服务心跳包：
             * 1、清空全部巡检服务心跳；
             * 2、重新发送巡检服务心跳；
             *
             * 这样做的目的：
             * 当handleMessage方法处理 MSG_PATROL_SERVICE_HEARTBEAT 事件时
             * 它表示在当前页至少有N秒没有接收到MSG_PATROL_SERVICE_HEARTBEAT事件，此时需要通过特殊逻辑确保JD巡检服务正确进行；
             * 特殊逻辑详见：handleMessage -> MSG_PATROL_SERVICE_HEARTBEAT 处理
             */
            clearMessages(MSG_PATROL_SERVICE_HEARTBEAT) // 1)
            sendEmptyMessageDelayed(MSG_PATROL_SERVICE_HEARTBEAT, PATROL_SERVICE_HEARTBEAT_INTERVAL) // 2)
        }

        /** 清空事件 */
        /*package*/ fun clearMessages(what: Int) {
            if (hasMessages(what)) {
                removeMessages(what)
            }
        }

        /** 巡检服务中断/退出 */
        /*package*/ fun onQuit() {
            clearMessages(MSG_PATROL_SERVICE_HEARTBEAT) // 清空巡检服务心跳事件
            // 销毁Handler线程
            looper.apply {
                try {
                    quit()
                } catch (exception: Exception) {
                    // ignore
                } catch (throwable: Throwable) {
                    // ignore
                }
            }
        }
    }

    /** 服务中断时回调 */
    override fun onInterrupt() {
        Log.d(TAG, ":-> Shutdown JD patrol service on interrupt.")

        mPatrolWatcher.onQuit() // 中断时退出
    }

    /** 成功断开无障碍服务时回调 */
    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, ":-> Shutdown JD patrol service on unbind.")

        mPatrolWatcher.onQuit() // 结束时退出
        return super.onUnbind(intent)
    }

    /** 成功连接无障碍服务时回调 */
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, ":-> Start JD patrol service on connected. ")
        // 创建页面刷新检查器
        HandlerThread(WATCHER_THREAD_NAME).apply {
            start() // 启动工作线程
            mPatrolWatcher = PatrolWatcher(looper) // 创建页面刷新检查器对象
            mPatrolWatcher.sendPatrolServiceHeartBeat() // 发送巡检服务心跳包
        }
        // 创建可绘制组件
        makeDrawableViewOnWindow()
    }

    /** 在window上创建可绘制的组件 */
    private fun makeDrawableViewOnWindow() {
        (getSystemService(Context.WINDOW_SERVICE) as WindowManager).apply {
            // 1、设置window属性
            val lp = WindowManager.LayoutParams()
            // width and height
            lp.width = ScreenUtils.getScreenWidth()
            lp.height = ScreenUtils.getScreenHeight()
            // gravity
            lp.gravity = Gravity.START or Gravity.TOP
            // format
            lp.format = PixelFormat.RGBA_8888
            // type
            lp.type = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            } else {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            }
            // flags
            lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                       WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                       WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            // 2、添加DrawLayoutBoundsView组件
            LayoutInflater.from(baseContext).apply {
                viewJDPatrolDrawPanel = inflate(com.mb.scrapbook.app.R.layout.view_jd_patrol_draw_panel, null)
                addView(viewJDPatrolDrawPanel, lp)
            }
        }
    }

    /** 读取调试Accessibility标识 */
    private fun readDebugAccessibility(): Boolean = JDPatrolSettingsUtils.searchConfigValue(
                                            TYPE_SETTINGS_PRINT_ACCESSIBILITY, TYPE_SETTINGS)?.let {
            TOGGLE_ON == it // 开关：开
    } ?: DEBUG_ACCESSIBILITY

    /** 事件监听回调 */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            if (readDebugAccessibility()) {
                event.source?.apply {
                    val viewRect = Rect()
                    getBoundsInScreen(viewRect)
                    Log.i( TAG, "${AccessibilityEvent.eventTypeToString(event.eventType)}, ${event.className}, $viewRect")
                }
            }
            // 发送巡检服务心跳包，此时确保JD巡检服务存活
            mPatrolWatcher.sendPatrolServiceHeartBeat()

            // 通知JD巡检绘制面板
            (viewJDPatrolDrawPanel as? JDPatrolDrawPanelView)?.apply {
                this.onReceiveEventNode(event, rootInActiveWindow) // 保留全部事件
                when (it.eventType) {
                    // 页面切换
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                        // 处理 TYPE_WINDOW_STATE_CHANGED 事件
                        this.onWindowStateChanged(event, rootInActiveWindow)
                    }
                    // 点击组件
                    AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                        // 处理 AccessibilityEvent.TYPE_VIEW_CLICKED 事件
                        this.onViewClicked(event, rootInActiveWindow)
                    }
                    // 滚动组件
                    AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                        // 处理 AccessibilityEvent.TYPE_VIEW_SCROLLED 事件
                        this.onViewScrolled(event, rootInActiveWindow)
                    }
                    // 页面刷新
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                        // 处理 TYPE_WINDOW_CONTENT_CHANGED 事件
                        this.onWindowContentChanged(event, rootInActiveWindow)
                    }
                }
            }
        }
    }

}