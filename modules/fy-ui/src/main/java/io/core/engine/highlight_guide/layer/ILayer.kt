package io.core.engine.highlight_guide.layer

import android.view.View
import io.core.engine.highlight_guide.controller.Location

/**
 * Desc: 蒙层接口
 *
 * Date: 2025/1/23 11:48
 */
interface ILayer {
    fun addHighlight(view: View): BaseLayer
    fun withView(
        view: View,
        verticalOffset: Int = 0,
        horizontalOffset: Int = 0,
        vararg locations: Location
    ): BaseLayer

    fun withImage(imgSrc: Int): BaseLayer
}
