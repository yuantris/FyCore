package io.core.engine.highlight_guide.layer

import android.content.Context
import android.widget.ImageView
import android.widget.RelativeLayout
import io.core.common.util.extensions.cool.dpToPx
import io.core.engine.highlight_guide.painter.CommonPainter
import io.core.engine.highlight_guide.controller.Location

/**
 * Desc: 通用蒙层
 * 附加视图规则
 * 居右布局， 如果屏幕宽度大于素材宽度，则固定尺寸368dp；
 * 如果屏幕宽度小于等于素材宽度，则等比例缩小；
 *
 * Date: 2025/1/21 15:30
 */
class CommonLayer(context: Context) : BaseLayer(context) {
    override fun providePainter() = CommonPainter()

    override fun withImage(imgSrc: Int): BaseLayer {
        val imageView = ImageView(context)
        val params = RelativeLayout.LayoutParams(
            368.dpToPx(),
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        imageView.layoutParams = params
        imageView.adjustViewBounds = true
        imageView.setImageResource(imgSrc)

        withView(
            imageView,
            10.dpToPx(),
            0,
            Location.TO_BOTTOM,
            Location.ALIGN_PARENT_RIGHT
        )
        return this
    }

    override fun onShow() {
        //无行为
    }

    override fun onDismiss() {
        //无行为
    }
}
