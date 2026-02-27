package com.core.fy.android.widget

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.Toolbar
import com.blankj.utilcode.util.BarUtils
import com.core.fy.android.R

/**
# ██████████
# █▄█████▄�?
# █▼▼▼▼▼
# �?
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载…�?
 * 2025/1/8 19:38
 * @description
 * @author Yuan
 */
class XToolbar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : Toolbar(context, attrs, defStyleAttr) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val totalHeight = if (isInEditMode) {
            // �?XML 预览时设置一个默认高�?(�?120)
            120
        } else {
            // 实际运行时动态计算高�?
            // 获取 StatusBar �?ActionBar 的高�?
            val statusBarHeight = BarUtils.getStatusBarHeight()
            val actionBarHeight = BarUtils.getActionBarHeight()
            statusBarHeight + actionBarHeight
        }

        // 创建一个新�?MeasureSpec 来强制设置高�?
        val newHeightMeasureSpec = MeasureSpec.makeMeasureSpec(totalHeight, MeasureSpec.EXACTLY)

        // 调用父类�?onMeasure 方法，使用新的高�?MeasureSpec
        super.onMeasure(widthMeasureSpec, newHeightMeasureSpec)
    }


}