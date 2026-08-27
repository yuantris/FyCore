package io.core.engine.highlight_guide

import android.graphics.Rect
import android.view.View

/**
 * Desc: 高亮引导工具类
 *
 * Date: 2025/1/23 10:51
 */
object HighLightUtils {
    fun getViewAbsRect(view: View): Rect {
        val locView = IntArray(2)
        view.getLocationOnScreen(locView)
        return Rect().apply {
            set(
                locView[0],
                locView[1],
                locView[0] + view.measuredWidth,
                locView[1] + view.measuredHeight
            )
        }
    }
}
