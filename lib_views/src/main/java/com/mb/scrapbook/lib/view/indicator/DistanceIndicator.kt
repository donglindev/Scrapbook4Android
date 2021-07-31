package com.mb.scrapbook.lib.view.indicator

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.SizeUtils
import com.mb.scrapbook.lib.view.R
import kotlin.math.max

/**
 * 带距离效果的指示器
 *
 * @author DongLin
 * @date 2021/07/30
 */
open class DistanceIndicator(context: Context, set: AttributeSet) : AppCompatTextView(context, set) {

    companion object {
        const val TAG = "DistanceIndicator";
        const val defaultWidth = 32f // 25dp
        const val defaultHeight = 4f // 4dp
    }

    /**
     * 指示器自己的宽
     */
    private var indicatorWidth: Float = 0f

    /**
     * 指示器自己的高
     */
    private var indicatorHeight: Float = 0f

    /**
     * 指示器背景颜色（长条部分）
     */
    private var indicatorBgColor: Int = 0

    /**
     * 指示器条颜色（短条部分）
     */
    private var indicatorColor: Int = 0

    /**
     * 指示器背景条矩形
     */
    private var indicatorBgRectF: RectF = RectF()

    /**
     * 指示器矩形
     */
    private var indicatorRectF: RectF = RectF()

    /**
     * 移动的起始位置
     */
    private var startOffset: Float = 0f

    /**
     * 指示器大小比例
     */
    private var sizeRatio: Float = 0.70f


    /**
     * 更新距离属性
     * offset: 修改起始位置偏移量: start=x+offset
     * ratio: 指示器大小尺寸比例: size=width * ratio
     */
    fun onUpdateDistance(offset: Float, ratio: Float) {
        if (offset != startOffset || ratio != sizeRatio) {
            startOffset = offset
            sizeRatio = ratio
            requestLayout()
        }
    }


    init {
        // 初始化指示器相关属性
        val attrs = context.obtainStyledAttributes(
                        set, R.styleable.DistanceIndicatorStyle
        ).apply {
            indicatorWidth = indicatorWidth.run {
                val width = getDimension(R.styleable.DistanceIndicatorStyle_indicator_width, defaultWidth)
                SizeUtils.dp2px(width).toFloat()
            }

            indicatorHeight = indicatorHeight.run {
                val height = getDimension(R.styleable.DistanceIndicatorStyle_indicator_height, defaultHeight)
                SizeUtils.dp2px(height).toFloat()
            }

            val defaultIndColor = ContextCompat.getColor(context, android.R.color.holo_purple)
            indicatorColor = getColor(R.styleable.DistanceIndicatorStyle_indicator_color, defaultIndColor)

            val defaultBgColor = ContextCompat.getColor(context, android.R.color.white)
            indicatorBgColor = getColor(R.styleable.DistanceIndicatorStyle_indicator_bg_color, defaultBgColor)

        }
        attrs.recycle()
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val widthSize = max(MeasureSpec.getSize(widthMeasureSpec).toFloat(), indicatorWidth)
        val heightSize = max(MeasureSpec.getSize(heightMeasureSpec).toFloat(), indicatorHeight)

        val x: Float = widthSize / 2 - indicatorWidth / 2
        val y: Float = heightSize / 2 - indicatorHeight / 2

        indicatorBgRectF?.let {
            it.left = x
            it.top = y
            it.right = x + indicatorWidth
            it.bottom = y + indicatorHeight
        }

        indicatorRectF?.let {
            it.left = indicatorBgRectF.left
            it.top = indicatorBgRectF.top
            it.right = indicatorBgRectF.right
            it.bottom = indicatorBgRectF.bottom
        }
    }


    /**
     * 绘制指示器
     */
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        // 绘制指示器总进度条
        drawIndicatorBackground(canvas)
        // 绘制指示器进度条
        drawIndicator(canvas)
    }


    /**
     * 绘制指示器进度条
     */
    private fun drawIndicator(canvas: Canvas?) {
        canvas?.save()
        indicatorRectF?.let {
            it.left = it.left + startOffset
            it.right = it.left + indicatorWidth * sizeRatio
        }
        val paint: Paint = paint;
        paint.color = indicatorColor
        canvas?.drawRect(indicatorRectF, paint)
        canvas?.restore()
    }


    /**
     * 绘制指示器总进度条
     */
    protected open fun drawIndicatorBackground(canvas: Canvas?) {
        canvas?.save()
        val paint: Paint = paint;
        paint.color = indicatorBgColor
        canvas?.drawRect(indicatorBgRectF, paint)
        canvas?.restore()
    }
}