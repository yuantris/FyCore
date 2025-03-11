package io.core.engine.highlight_guide.controller

import android.app.Activity
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import io.core.engine.highlight_guide.layer.BaseLayer
import io.core.engine.highlight_guide.layer.ILayerController
import java.util.LinkedList
import java.util.Queue

/**
 * Desc: 高亮引导管理器
 *
 * Date: 2025/1/21 14:40
 */
class HighlightGuideManager(val parentView: FrameLayout) : ILayerController {
    constructor(activity: Activity) : this(activity.window.decorView as FrameLayout)
    constructor(fragment: Fragment) : this(fragment.requireView().rootView as FrameLayout)

    private val layerQueue: Queue<BaseLayer> = LinkedList()
    private lateinit var currentLayer: BaseLayer

    override fun dismissCurrent() {
        if (::currentLayer.isInitialized) {
            currentLayer.onDismiss()
            parentView.removeView(currentLayer.getView())
            layerQueue.poll()
        }
    }

    override fun showNext() {
        dismissCurrent()
        show()
    }

    override fun show() {
        if (layerQueue.isNotEmpty()) {
            currentLayer = layerQueue.peek()!!
            parentView.post {
                parentView.addView(
                    currentLayer.getView(),
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
                currentLayer.onShow()
            }
        }
    }

    fun addLayer(layer: BaseLayer): HighlightGuideManager {
        layer.controller = this
        layerQueue.add(layer)
        return this
    }
}
