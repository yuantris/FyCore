package io.core.widget.view

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.WindowInsets
import androidx.annotation.RequiresApi
import io.core.common.util.log.LogPure
import kotlin.apply

class NavBarPlaceholderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {


    init {
        val defaultNavBarHeight = getDefaultNavBarHeight()

        // 设置初始最小高度
        minimumHeight = defaultNavBarHeight

        // 检查是否处于预览模式
        if (isInEditMode) {
            // 在预览模式下使用固定高度
            setHeight(defaultNavBarHeight)
        } else {
            // 根据版本使用不同的方式获取导航栏高度
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setupWindowInsetsListener()
            } else {
                // 对于低版本，使用默认高度
                setHeight(defaultNavBarHeight)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun setupWindowInsetsListener() {
        setOnApplyWindowInsetsListener { _, insets ->
            val navBarHeight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // API 30+ 使用新方法
                val navigationBarInsets = insets.getInsets(WindowInsets.Type.navigationBars())
                LogPure.e("NavBarPlaceholderView", "navigationBarInsets: left=${navigationBarInsets.left}, top=${navigationBarInsets.top}, right=${navigationBarInsets.right}, bottom=${navigationBarInsets.bottom}")
                navigationBarInsets.bottom
            } else {
                // API 29 使用旧方法
                val systemWindowInsetBottom = insets.systemWindowInsets.bottom
                LogPure.e("NavBarPlaceholderView", "systemWindowInsets.bottom: $systemWindowInsetBottom")
                systemWindowInsetBottom
            }

            // 添加一个检查，如果高度为0则使用默认高度
            val finalHeight = if (navBarHeight > 0) navBarHeight else getDefaultNavBarHeight()
            LogPure.e("NavBarPlaceholderView", "navBarHeight: $navBarHeight, finalHeight: $finalHeight")

            setHeight(finalHeight)
            insets
        }

        // 强制请求应用 insets
        requestApplyInsets()
    }


    private fun setHeight(height: Int) {
        minimumHeight = height
        layoutParams = layoutParams?.apply {
            this.height = height
        }
    }

    @SuppressLint("InternalInsetResource")
    private fun getDefaultNavBarHeight(): Int {
        val resources = context.resources
        val resourceId = resources.getIdentifier(
            "navigation_bar_height",
            "dimen",
            "android"
        )
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            // 默认高度
            48.dpToPx()
        }
    }

    private fun Int.dpToPx(): Int {
        val density = context.resources.displayMetrics.density
        return (this * density).toInt()
    }
}