package io.core.engine.highlight_guide.layer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.RelativeLayout
import io.core.engine.highlight_guide.HighLightUtils
import io.core.engine.highlight_guide.controller.Location
import kotlin.math.abs

/**
 * Desc: 蒙版View，实际引导渲染的View
 * 具体绘制委托给外界操作，不同业务绘制实现不同
 * 处理附加视图的位置控制，以及点击事件
 *
 * Date: 2025/1/21 18:11
 */
class LayerView : RelativeLayout {
    private val targetRectList = mutableListOf<Rect>()

    var backColor = 0x60000000
    private val paint = Paint()
    private val porterDuffXMode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    private var downX = 0f
    private var downY = 0f

    var drawCallBack:
            ((context: Context, rectList: List<Rect>, canvas: Canvas, paint: Paint) -> Unit)? = null
    var targetClickListener: ((index: Int) -> Unit)? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val rectSelf = HighLightUtils.getViewAbsRect(this)
        val childCount = this.childCount
        for (i in 0 until childCount) {
            val child = this.getChildAt(i)
            val locBean = child.tag as LocBean
            if (locBean.targetIndex !in 0 until targetRectList.size) {
                continue
            }
            val targetRect = targetRectList[locBean.targetIndex]
            val verticalAxis = (targetRect.left + targetRect.right) / 2
            val horizontalAxis = (targetRect.top + targetRect.bottom) / 2
            val width = child.measuredWidth
            val height = child.measuredHeight
            var top = 0
            var bottom = 0
            var left = 0
            var right = 0
            locBean.locateList.forEach {
                when (it) {
                    Location.TO_TOP -> {
                        left = if (left == 0) verticalAxis - width / 2 else left
                        top = targetRect.top - height
                        right = if (right == 0) verticalAxis + width / 2 else right
                        bottom = targetRect.top
                    }

                    Location.TO_BOTTOM -> {
                        left = if (left == 0) verticalAxis - width / 2 else left
                        top = targetRect.bottom
                        right = if (right == 0) verticalAxis + width / 2 else right
                        bottom = targetRect.bottom + height
                    }

                    Location.TO_LEFT -> {
                        left = targetRect.left - width
                        top = if (top == 0) horizontalAxis - height / 2 else top
                        right = targetRect.left
                        bottom = if (bottom == 0) horizontalAxis + height / 2 else bottom
                    }

                    Location.TO_RIGHT -> {
                        left = targetRect.right
                        top = if (top == 0) horizontalAxis - height / 2 else top
                        right = targetRect.right + width
                        bottom = if (bottom == 0) horizontalAxis + height / 2 else bottom
                    }

                    Location.COVER -> {
                        left = targetRect.left
                        top = targetRect.top
                        right = targetRect.right
                        bottom = targetRect.bottom
                    }

                    Location.ALIGN_TOP -> {
                        left = if (left == 0) verticalAxis - width / 2 else left
                        top = targetRect.top
                        right = if (right == 0) verticalAxis + width / 2 else right
                        bottom = targetRect.top + height
                    }

                    Location.ALIGN_BOTTOM -> {
                        left = if (left == 0) verticalAxis - width / 2 else left
                        top = targetRect.bottom - height
                        right = if (right == 0) verticalAxis + width / 2 else right
                        bottom = targetRect.bottom
                    }

                    Location.ALIGN_LEFT -> {
                        left = targetRect.left
                        top = if (top == 0) horizontalAxis - height / 2 else top
                        right = targetRect.left + width
                        bottom = if (bottom == 0) horizontalAxis + height / 2 else bottom
                    }

                    Location.ALIGN_RIGHT -> {
                        left = targetRect.right - width
                        top = if (top == 0) horizontalAxis - height / 2 else top
                        right = targetRect.right
                        bottom = if (bottom == 0) horizontalAxis + height / 2 else bottom
                    }

                    Location.ALIGN_PARENT_RIGHT -> {
                        left = rectSelf.right - width
                        top = if (top == 0) horizontalAxis - height / 2 else top
                        right = rectSelf.right
                        bottom = if (bottom == 0) horizontalAxis + height / 2 else bottom
                    }
                }
            }

            child.layout(
                left + locBean.horizontalOffset - rectSelf.left,
                top + locBean.verticalOffset - rectSelf.top,
                right + locBean.horizontalOffset - rectSelf.left,
                bottom + locBean.verticalOffset - rectSelf.top
            )
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        canvas.drawColor(backColor)
        paint.reset()
        paint.isAntiAlias = true
        paint.xfermode = porterDuffXMode
        val offset = HighLightUtils.getViewAbsRect(this)
        //将矩形的坐标从全局坐标系转换到 layerView 的局部坐标系中，以便在canvas上绘制矩形�?
        val rectList = targetRectList.map {
            it.offset(-offset.left, -offset.top)
            it
        }
        drawCallBack?.invoke(context, rectList, canvas, paint)
        super.dispatchDraw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                return true
            }

            MotionEvent.ACTION_UP -> {
                performClick()
                val upX = event.x
                val upY = event.y
                if (abs(upX - downX) < 10 && abs(upY - downY) < 10) {
                    targetClickListener?.let {
                        for ((index, value) in targetRectList.withIndex()) {
                            if (value.contains(upX, upY)) {
                                it.invoke(index)
                                return true
                            }
                        }
                        it.invoke(-1)
                        return true
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }

    fun addTargetsRect(rect: Rect) {
        targetRectList.add(rect)
    }

    fun addExtraView(
        view: View,
        targetIndex: Int,
        verticalOffset: Int,
        horizontalOffset: Int,
        locateList: List<Location>
    ) {
        view.tag = LocBean(targetIndex, locateList, verticalOffset, horizontalOffset)
        addView(view)
    }

    private fun Rect.contains(x: Float, y: Float): Boolean {
        return (left < right && top < bottom && x >= left && x < right && y >= top && y < bottom)
    }

    private data class LocBean(
        var targetIndex: Int,
        var locateList: List<Location>,
        var verticalOffset: Int,
        var horizontalOffset: Int
    )
}
