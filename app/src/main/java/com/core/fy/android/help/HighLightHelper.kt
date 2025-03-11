package com.core.fy.android.help

import android.app.Activity
import android.view.View
import androidx.fragment.app.Fragment
import com.core.fy.android.R
import io.core.engine.highlight_guide.controller.HighlightGuideManager
import io.core.engine.highlight_guide.layer.CommonLayer

/**
 * Desc: 高亮引导帮助类
 * <p>
 * Date: 2025/1/23 16:52
 */
object HighLightHelper {
    fun showQuickFolderGuide(
        activity: Activity,
        view1: View,
        view2: View,
    ) {
        HighlightGuideManager(activity)
            .addLayer(
                CommonLayer(activity)
                    .addHighlight(view1)
                    .addHighlight(view2)
            ).addLayer(
                CommonLayer(activity)
                    .addHighlight(view2)
                    .withImage(R.mipmap.ic_launcher)
            ).show()
    }

    fun showQuickFolderGuide(
        fragment: Fragment,
        view1: View,
        view2: View,
    ) {
        HighlightGuideManager(fragment)
            .addLayer(
                CommonLayer(fragment.requireContext())
                    .addHighlight(view1)
                    .addHighlight(view2)
            ).addLayer(
                CommonLayer(fragment.requireContext())
                    .addHighlight(view2)
            ).show()
    }

}
