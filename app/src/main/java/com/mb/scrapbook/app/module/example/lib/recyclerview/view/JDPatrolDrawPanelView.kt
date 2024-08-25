package com.mb.scrapbook.app.module.example.lib.recyclerview.view

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ImageView
import android.widget.TextView
import com.mb.scrapbook.app.module.example.lib.recyclerview.model.JDPatrolManagerService
import com.mb.scrapbook.app.module.example.lib.recyclerview.model.JDPatrolManagerService.PagePatrol.DolphinActivityPatrol.Companion.STACK_CONTAINER_SCROLL
import com.mb.scrapbook.app.module.example.lib.recyclerview.model.JDPatrolManagerService.PagePatrol.DolphinActivityPatrol.Companion.STACK_SCREENSHOT
import com.mb.scrapbook.app.module.example.lib.recyclerview.model.JDPatrolSettingsUtils
import com.mb.scrapbook.lib.base.model.AccessibilityUtils
import com.mb.scrapbook.lib.view.DrawLayoutBoundsView
import java.util.concurrent.LinkedBlockingDeque

import com.mb.scrapbook.app.module.example.lib.recyclerview.repository.JDPatrolSettingsRepository.Companion.TOGGLE_ON
import com.mb.scrapbook.app.module.example.lib.recyclerview.repository.JDPatrolSettingsRepository.Companion.TYPE_SETTINGS
import com.mb.scrapbook.app.module.example.lib.recyclerview.repository.JDPatrolSettingsRepository.Companion.TYPE_SETTINGS_PRINT_NODE
import com.mb.scrapbook.lib.base.mvvm.view.ScreenshotActivity
import java.util.concurrent.atomic.AtomicInteger

/**
 * JD Patrol Draw Panel View
 * 京东巡检绘画面板组件
 *
 * @author donglin6
 * @date 2024/06/12
 */
class JDPatrolDrawPanelView: DrawLayoutBoundsView, JDPatrolManagerService.JDPagePatrolListener {

    /** 伴生对象 */
    companion object {
        // 日志
        private const val TAG = "donglin"
        /** 工作线程名称 */
        private const val WATCHER_THREAD_NAME = "JDPatrolDraw"
        /** 页面栈容量 */
        private const val PAGE_STACK_CAPACITY = 3
        // 调试：打印节点信息
        private const val DEBUG_PRINT_NODE = false
        // 调试：绘制节点矩形
        private const val DEBUG_DRAW_NODE = false

        /** 100毫秒绘制一次界面节点 */
        private const val DRAW_NODE_RECT_ON_PAGE_DURATION = 200L
        /**
         * 是否允许在Window上绘制组件区域
         * 每间隔 DRAW_NODE_RECT_ON_PAGE_DURATION 时间可绘制一次组件区域，可有效降低绘制频率，提升巡检性能；
         */
        private const val MSG_DRAW_NODE_RECT_ON_WINDOW = 0x00A0

        /** 3秒后开始巡检 */
        private const val COUNT_DOWN_START_PATROL_WHEN_PAGE_CHANGED_DURATION = 3000L
        /**
         * 倒计时 COUNT_DOWN_START_PATROL_WHEN_PAGE_CHANGED_DURATION 后开始巡检页面
         * 当Handler处理此消息时，说明页面切换后停留了 COUNT_DOWN_START_PATROL_WHEN_PAGE_CHANGED_DURATION 时长，
         * 所以，此时可以开启自动巡检逻辑了，巡检逻辑使用的node对象由 onReceiveEventNode 方法提供；
         */
        private const val MSG_COUNT_DOWN_START_PATROL_WHEN_PAGE_CHANGED = 0x00A1
    }

    /** 内部 */
    private inner class H(looper: Looper) : Handler(looper) {

        /** 自动巡检标识 */
        private val stateAutoPatrol: AtomicInteger = AtomicInteger(0)

        /** 处理消息 */
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                /*
                 * 处理自动巡检标识
                 * 当触发该消息时，表示切换页面已经达到 COUNT_DOWN_START_PATROL_WHEN_PAGE_CHANGED_DURATION 时长
                 * 可设置自动巡检标识为true，表示可以开启自动巡检逻辑，同时启动自动巡检功能；
                 */
                MSG_COUNT_DOWN_START_PATROL_WHEN_PAGE_CHANGED -> {
                    if (stateAutoPatrol.compareAndSet(0, 1)) {
                        onStartAutoPatrolCompleted() // 启动自动巡检
                    }
                }
            }
        }

        /*
         * 每次页面切换（WINDOW_STATE_CHANGED）后回调此方法
         * 该方法发送延时（COUNT_DOWN_START_PATROL_WHEN_PAGE_CHANGED_DURATION）消息，当handleMessage方法开
         * 始处理该消息时，表示页面切换后已经停留了 COUNT_DOWN_START_PATROL_WHEN_PAGE_CHANGED_DURATION 时长，
         * 此时设置巡检标识位为“自动巡检”模式，巡检逻辑使用的node对象由 onReceiveEventNode 方法提供;
         */
        /*package*/ fun sendStartPatrolWhenPageChanged() {
            // 重新计时：先清空 MSG_COUNT_DOWN_START_PATROL_WHEN_PAGE_CHANGED 已存在的事件
            if (hasMessages(MSG_COUNT_DOWN_START_PATROL_WHEN_PAGE_CHANGED)) {
                removeMessages(MSG_COUNT_DOWN_START_PATROL_WHEN_PAGE_CHANGED)
            }
            // 当页面发生改变或点击组件后，要尽快关闭自动巡检逻辑避免出现问题
            stateAutoPatrol.compareAndSet(1, 0)
            // 发送自动巡检倒计时事件
            sendEmptyMessageDelayed(MSG_COUNT_DOWN_START_PATROL_WHEN_PAGE_CHANGED, // 自动巡检
                                    COUNT_DOWN_START_PATROL_WHEN_PAGE_CHANGED_DURATION) // delay 3/s
        }

        /** 每间隔 DRAW_NODE_RECT_ON_PAGE_DURATION 毫秒可绘制一次组件区域 */
        /*package*/ fun drawNodeRectIfNecessary(): Boolean {
            val drawableNode = !hasMessages(MSG_DRAW_NODE_RECT_ON_WINDOW)
            if (drawableNode) {
                sendEmptyMessageDelayed(MSG_DRAW_NODE_RECT_ON_WINDOW, DRAW_NODE_RECT_ON_PAGE_DURATION)
            }
            return drawableNode
        }

        /** 当前是否已开启自动巡检逻辑 */
        /*package*/ fun shouldStartAutoPatrol() = stateAutoPatrol.compareAndSet(1, stateAutoPatrol.get())
    }

    /** 文本提示组件 */
    private lateinit var mTextTips: TextView
    /** 图片提示组件 */
    private lateinit var mImageLoading: ImageView

    /** 页面栈 */
    private val mPageStack: LinkedBlockingDeque<String> = LinkedBlockingDeque(PAGE_STACK_CAPACITY)
    /** 工作线程Handler对象 */
    private lateinit var mH: H
    /** 京东巡检管理服务 */
    private lateinit var managerPatrol: JDPatrolManagerService

    /** 构造方法 */
    constructor(context: Context, attrs: AttributeSet): this(context, attrs, 0)

    /** 构造方法 */
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    /** 初始化 */
    override fun onFinishInflate() {
        super.onFinishInflate()
        // 初始化对象
        managerPatrol = JDPatrolManagerService(this)
        // 初始化组件
        mTextTips = findViewById(com.mb.scrapbook.app.R.id.textTips)
        mImageLoading = findViewById(com.mb.scrapbook.app.R.id.imageLoading)
        // 创建Handler对象
        mH = H(Looper.getMainLooper())
    }

    /** 更新文本提示组件显示状态 */
    private fun onUpdateTextTips(display: Boolean) {
        mTextTips.apply { visibility = if (display) View.VISIBLE else View.GONE }
    }

    /** 更新图片提示组件显示状态 */
    private fun onUpdateImageLoading(display: Boolean) {
        mImageLoading.apply { visibility = if (display) View.VISIBLE else View.GONE }
    }

    /**
     * 启动自动巡检功能
     * 该方法位于 H.MSG_COUNT_DOWN_START_PATROL_WHEN_PAGE_CHANGED 消息处理中调用
     * 该方法被调用，此时表示页面可进入自动巡检流程，同时启动自动巡检流程；
     */
    private fun onStartAutoPatrolCompleted() {
        if (mH.shouldStartAutoPatrol()) {
            runAutoPatrol() // 启动自动巡检流程
        }
    }

    /**
     * 启动自动巡检流程
     */
    private fun runAutoPatrol() {
        mPageStack.peekLast()?.let { page -> // 当前页有效
            if (mH.shouldStartAutoPatrol()) {
                val nodeEvent = mOnReceiveLastEventNode
                val patrol = managerPatrol.makePagePatrol(page)
                patrol.run(context, nodeEvent)
            }
        }
    }

    /** 绘制对象 */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
    }

    /** 处理 AccessibilityEvent.TYPE_VIEW_CLICKED 事件 */
    fun onViewClicked(event: AccessibilityEvent?,
                      rootInActiveWindow: AccessibilityNodeInfo?) {
        event?.let {
            Log.d(TAG, "onViewClicked:-> ${ it.className }")
        }
    }

    /** 处理 AccessibilityEvent.TYPE_VIEW_SCROLLED 事件 */
    fun onViewScrolled(event: AccessibilityEvent?,
                       rootInActiveWindow: AccessibilityNodeInfo?) {
        event?.let {
            Log.d(TAG, "onViewScrolled:-> ${ it.className }")
            mPageStack.peekLast()?.let { page -> // 当前页有效
                managerPatrol.makePagePatrol(page).let { patrol ->
                    when (patrol) {
                        is JDPatrolManagerService.PagePatrol.DolphinActivityPatrol -> {
                            if (patrol.inPatrolStack(STACK_CONTAINER_SCROLL)
                                    || patrol.inPatrolStack(STACK_SCREENSHOT)) {
                                patrol.run(context, rootInActiveWindow)
                            }
                        }
                        else -> { }
                    }
                }
            }
        }
    }

    /** 处理 AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED 事件 */
    fun onWindowStateChanged(event: AccessibilityEvent?,
                             rootInActiveWindow: AccessibilityNodeInfo?) {
        // 将class name保存至页面栈中
        event?.apply {
            className?.let { itClassName ->
                // className有效：非空，非空白，并且非android.widget开头
                if (itClassName.isNotEmpty() && itClassName.isNotBlank()
                        && !itClassName.startsWith("android.widget", true)) {
                    Log.d(TAG, "onWindowStateChanged:-> $itClassName")
                    if (mPageStack.remainingCapacity() == 0) { // 栈已满
                        mPageStack.pollFirst() // 移除栈底元素
                    }
                    /*
                     * 页面切换后，页面名称由className对象保存，并处理以下逻辑：
                     * 1、将新页面保存值页面栈（mPageStack）中；
                     * 2、页面切换后申请绘制节点，因为有些页面中组件不包含自动逻辑，所以无法回调WindowContentChange事件，所以需要通过页面切换申请绘制矩形；
                     * 3、页面切换3秒后开启自动巡检功能，自动巡检逻辑支持： 登录 -> 打开京东国际页 -> 自动巡检
                     */
                    // 1、将class name装入栈顶
                    mPageStack.offerLast(itClassName.toString())
                    // 2、页面切换后，绘制节点矩形
                    drawNodeRectOnWindow(event, rootInActiveWindow)
                    // 3、页面切换后，启动自动巡检倒计时
                    mH.sendStartPatrolWhenPageChanged()
                }
            }
        }
    }

    /** 处理 AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED 事件 */
    fun onWindowContentChanged(event: AccessibilityEvent?,
                               rootInActiveWindow: AccessibilityNodeInfo?) {
        // 页面内容改变后，绘制节点矩形
        drawNodeRectOnWindow(event, rootInActiveWindow)
    }

    /** 绘制节点矩形 */
    private fun drawNodeRectOnWindow(event: AccessibilityEvent?,
                                     rootInActiveWindow: AccessibilityNodeInfo?) {
        onUpdateTextTips(false) // 关闭文本提示组件
        onUpdateImageLoading(false) // 关闭图片提示组件
        if (DEBUG_DRAW_NODE && mH.drawNodeRectIfNecessary()) {
            rootInActiveWindow?.apply {
                val drawNode = AccessibilityUtils.makeRectNode(this)
                drawNode?.let { node ->
                    if (readDebugPrintNode()) {
                        Log.i(TAG, "---------------------------------------------------------")
                        AccessibilityUtils.debugNode(TAG, node)
                    }
                    updateLastDrawNode(node) // 父类绘制
                }
            }
        }
    }

    /** 读取调试Accessibility标识 */
    private fun readDebugPrintNode(): Boolean = JDPatrolSettingsUtils.searchConfigValue(
                                                    TYPE_SETTINGS_PRINT_NODE, TYPE_SETTINGS)?.let {
            TOGGLE_ON == it // 开关：开
    } ?: DEBUG_PRINT_NODE

}