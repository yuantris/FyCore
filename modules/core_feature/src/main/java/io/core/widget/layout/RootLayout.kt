package io.core.widget.layout

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.core.widget.NestedScrollView
import io.core.R
import io.core.common.util.ext.ui.gone
import io.core.common.util.ext.ui.statusBarHeight
import io.core.common.util.ext.ui.visible
import kotlin.math.abs

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/18 15:43
 * @description
 * @author Yuan
 */
class RootLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val titleBar: TitleBar
    private val scrollContainer: NestedScrollView
    private val contentContainer: LinearLayout

    init {
        orientation = VERTICAL

        // 初始化标题栏
        titleBar = TitleBar(context).apply {
            id = R.id.title_bar
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        addView(titleBar)

        // 初始化滚动容器
        scrollContainer = NestedScrollView(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, 0, 1.0f)
            overScrollMode = OVER_SCROLL_NEVER
        }

        // 内容容器保持垂直布局特性
        contentContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }

        scrollContainer.addView(contentContainer)
        addView(scrollContainer)

    }


    // 重写添加子View方法，保持原有布局特性
    override fun addView(child: View?, params: ViewGroup.LayoutParams?) {
        if (child?.id != R.id.title_bar && child != scrollContainer) {
            contentContainer.addView(child, params)
        } else {
            super.addView(child, params)
        }
    }

    fun getTitleBar(): TitleBar = titleBar

}