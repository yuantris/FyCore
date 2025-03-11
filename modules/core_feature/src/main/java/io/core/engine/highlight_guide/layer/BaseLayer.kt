package io.core.engine.highlight_guide.layer

import android.content.Context
import android.graphics.Rect
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import io.core.engine.highlight_guide.HighLightUtils
import io.core.engine.highlight_guide.painter.IPainter
import io.core.engine.highlight_guide.controller.Location

/**
 * Desc: 蒙层基类
 * 提供基本的蒙层能力：添加高亮、添加提示视图
 * 并协调LayerView 与 IPainter
 *
 * Date: 2025/1/21 15:08
 */
abstract class BaseLayer(val context: Context) : ILayer {
    internal lateinit var controller: ILayerController
    private var layerView: LayerView
    private val painter: IPainter by lazy { providePainter() }
    private var layerInit = false
    private var targetCounts = 0

    init {
        layerView = LayerView(context).apply {
            backColor = ContextCompat.getColor(context, painter.getBackgroundColor())
            drawCallBack = painter::onDraw
            targetClickListener = ::onClick
        }
    }

    fun getView(): View {
        if (!layerInit) {
            layerView.post {
                layerView.requestLayout()
            }
            layerInit = true
        }
        return layerView
    }

    override fun addHighlight(view: View): BaseLayer {
        targetCounts++
        view.post {
            addHighlight(HighLightUtils.getViewAbsRect(view))
        }
        return this
    }

    private fun addHighlight(rect: Rect): BaseLayer {
        targetCounts++
        layerView.addTargetsRect(rect)
        return this
    }

    override fun withView(
        view: View,
        verticalOffset: Int,
        horizontalOffset: Int,
        vararg locations: Location
    ): BaseLayer {
        layerView.addExtraView(
            view,
            targetCounts - 1,
            verticalOffset,
            horizontalOffset,
            locations.toList()
        )
        return this
    }

    override fun withImage(
        @DrawableRes imgSrc: Int
    ): BaseLayer {
        val imageView = ImageView(context)
        val params = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        imageView.layoutParams = params
        imageView.adjustViewBounds = true
        imageView.setImageResource(imgSrc)

        withView(imageView, 0, 0, Location.TO_BOTTOM)
        return this
    }

    open fun onClick(index: Int) {
        controller.showNext()
    }

    abstract fun providePainter(): IPainter
    abstract fun onShow()
    abstract fun onDismiss()
}
