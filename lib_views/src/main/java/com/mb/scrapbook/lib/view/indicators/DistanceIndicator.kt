package com.mb.scrapbook.lib.view.indicators

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.SizeUtils
import com.mb.scrapbook.lib.view.R

/**
 * 带距离效果的指示器
 *
 * @author DongLin
 * @date 2021/07/30
 */
class DistanceIndicator(context: Context, set: AttributeSet) : AppCompatTextView(context, set) {

    companion object {
        const val TAG = "DistanceIndicator";
        const val defaultWidth = 25f // 25dp
        const val defaultHeight = 4f // 4dp
    }

    /**
     * 指示器自己的宽
     */
    private var selfWidth: Float = 0f

    /**
     * 指示器自己的高
     */
    private var selfHeight: Float = 0f

    /**
     * 指示器背景颜色（长条部分）
     */
    private var selfColor: Int = 0

    /**
     * 指示器条宽
     */
    private var indWidth: Float = 0f

    /**
     * 指示器条高
     */
    private var indHeight: Float = 0f

    /**
     * 指示器条颜色（短条部分）
     */
    private var indColor: Int = 0

    /**
     * 是否绘制到中心位置
     */
    private var isCenter: Boolean = true;

    /**
     * 指示器画笔
     */
    private lateinit var selfPaint: Paint

    /**
     * 指示器条画笔
     */
    private lateinit var indPaint: Paint


    init {
        // 初始化指示器相关属性
        val attrs = context.obtainStyledAttributes(
                        set, R.styleable.DistanceIndicatorStyle
        ).apply {
            isCenter = getBoolean(R.styleable.DistanceIndicatorStyle_layout_center, true)
            val defaultBgColor = ContextCompat.getColor(context, android.R.color.white)
            selfColor = getColor(R.styleable.DistanceIndicatorStyle_self_bg_color, defaultBgColor)
            selfWidth = selfWidth.run {
                val width = getDimension(R.styleable.DistanceIndicatorStyle_self_width, defaultWidth)
                SizeUtils.dp2px(width).toFloat()
            }
            selfHeight = selfHeight.run {
                val height = getDimension(R.styleable.DistanceIndicatorStyle_self_height, defaultHeight)
                SizeUtils.dp2px(height).toFloat()
            }

            val defaultIndColor = ContextCompat.getColor(context, android.R.color.holo_purple)
            indColor = getColor(R.styleable.DistanceIndicatorStyle_ind_bg_color, defaultIndColor)
            indWidth = indWidth.run {
                val width = getDimension(R.styleable.DistanceIndicatorStyle_ind_width, selfWidth)
                SizeUtils.dp2px(width).toFloat()
            }
            indHeight = indHeight.run {
                val height = getDimension(R.styleable.DistanceIndicatorStyle_ind_height, selfHeight)
                SizeUtils.dp2px(height).toFloat()
            }
        }
        attrs.recycle()

        // 初始化画笔
        initPaint()
    }


    /**
     * 初始化画笔
     */
    private fun initPaint() {
        // 初始化指示器画笔
        selfPaint = Paint()
        selfPaint.style = Paint.Style.FILL
        selfPaint.isAntiAlias = true
        selfPaint.strokeWidth = selfHeight
        selfPaint.color = selfColor

        // 初始化指示器条画笔
        indPaint = Paint()
        indPaint.style = Paint.Style.FILL
        indPaint.isAntiAlias = true
        indPaint.strokeWidth = indHeight
        indPaint.color = indColor
    }


    /**
     * 绘制指示器
     */
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        // 绘制指示器总进度条
        drawSelf(canvas)
        // 绘制指示器进度条
        drawIndicator(canvas)
    }


    /**
     * 绘制指示器进度条
     */
    private fun drawIndicator(canvas: Canvas?) {
        canvas?.save()
        val indicatorRect = RectF().apply {
            if (isCenter) {
                left = width / 2 - selfWidth / 2
                top = height / 2 - selfHeight / 2
                right = left + selfWidth * 0.7f
                bottom = top + selfHeight
            } else {
                left = 0f
                top = 0f
                right = selfWidth * 0.7f
                bottom = selfHeight
            }
        }
        val paint: Paint = paint;
        paint.color = indColor
        canvas?.drawRect(indicatorRect, paint)
        canvas?.restore()
    }


    /**
     * 绘制指示器总进度条
     */
    private fun drawSelf(canvas: Canvas?) {
        canvas?.save()
        val selfRect = RectF().apply {
            if (isCenter) {
                left = width / 2 - selfWidth / 2
                top = height / 2 - selfHeight / 2
                right = left + selfWidth
                bottom = top + selfHeight
            } else {
                left = 0f
                top = 0f
                right = selfWidth
                bottom = selfHeight
            }
        }
        val paint: Paint = paint;
        paint.color = selfColor
        canvas?.drawRect(selfRect, paint)
        canvas?.restore()
    }
}