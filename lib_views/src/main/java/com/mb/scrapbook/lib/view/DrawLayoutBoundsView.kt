package com.mb.scrapbook.lib.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.SizeUtils
import com.mb.scrapbook.lib.base.model.AccessibilityRectNode

/**
 * Draw Layout Bounds View
 * 在组件上绘制AccessibilityRectNode对象
 *
 * @author donglin6
 * @date 2024/06/07
 */
open class DrawLayoutBoundsView: FrameLayout {

    /** 最后绘制节点 */
    private var mOnDrawLastRectNode: AccessibilityRectNode? = null
    /** 矩形边界画笔 */
    private lateinit var mBoundsPaint: Paint
    /** 无障碍服务发送的最后一个事件 */
    protected var mOnReceiveLastEventNode: AccessibilityNodeInfo? = null

    /** 构造方法 */
    constructor(context: Context, attrs: AttributeSet): this(context, attrs, 0)

    /** 构造方法 */
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    /** 绘制最后一个节点 */
    fun updateLastDrawNode(node: AccessibilityRectNode) {
        node.apply {
            mOnDrawLastRectNode = this // 更新数据
            invalidate()                      // 重新绘制（call onDraw）
        }
    }

    /** 接收AccessibilityEvent事件 */
    open fun onReceiveEventNode(event: AccessibilityEvent?,
                                rootInActiveWindow: AccessibilityNodeInfo?) {
        rootInActiveWindow?.apply { mOnReceiveLastEventNode = this }
    }

    /** 组件初始化完成 */
    override fun onFinishInflate() {
        super.onFinishInflate()
        // 初始化矩形边界画笔
        initBoundsPaint()
    }

    /** 初始化矩形边界画笔 */
    private fun initBoundsPaint() {
        mBoundsPaint = Paint()
        mBoundsPaint.isAntiAlias = true
        mBoundsPaint.style = Paint.Style.STROKE
        mBoundsPaint.strokeWidth = SizeUtils.dp2px(1f).toFloat()
        mBoundsPaint.color = Color.rgb(63, 127, 255)
    }

    /** 绘制节点入口方法 */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val statusBarHeight = BarUtils.getStatusBarHeight()
        mOnDrawLastRectNode?.apply {
            val stack = arrayListOf<AccessibilityRectNode>()
            stack.add(this) // 根元素

            while (stack.isNotEmpty()) {
                stack.removeLast().let { node ->
                    if (node.isVisibleToUser) {
                        val calTop = (node.rect.top - statusBarHeight)
                        val calBottom = (node.rect.bottom - statusBarHeight)

                        val l = node.rect.left
                        val r = node.rect.right
                        val t = if (calTop < 0) 0 else calTop
                        val b = if (calBottom < 0) 0 else calBottom
                        val rect = Rect(l, t, r, b)
                        canvas.drawRect(rect, mBoundsPaint)
                    }

                    // 向stack中添加子元素
                    node.childList.forEach { childItem ->
                        stack.add(childItem)
                    }
                }
            }
        }
    }
}