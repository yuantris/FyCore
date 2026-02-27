package io.core.engine.shape.span

import android.text.Layout
import android.text.style.AlignmentSpan
import android.text.style.ReplacementSpan
import android.view.Gravity
import android.view.View
import io.core.engine.shape.config.ITextViewAttribute


/**
# ██████████
# █▄█████▄�?
# █▼▼▼▼▼
# �?
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载…�?
 * 2025/1/21 10:59
 * @description
 * @author Yuan
 */
abstract class AlignmentReplacementSpan(val mTextAttribute: ITextViewAttribute) :
    ReplacementSpan(), AlignmentSpan {

    override fun getAlignment(): Layout.Alignment {
        // 获取 TextView 的文本重�?
        val gravity = mTextAttribute.getTextGravity()

        // 根据 gravity 设置 AlignmentSpan
        val alignment: Layout.Alignment

        // 获取当前布局方向（LTR �?RTL�?
        val isRtl = (mTextAttribute.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL)

        // 判断的顺序必须为：left �?right，start �?end，center �?center_horizontal
        alignment = if (hasFlag(gravity, Gravity.LEFT)) {
            // Gravity.LEFT 始终左对齐，需要根据布局方向判断
            if (isRtl) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL
        } else if (hasFlag(gravity, Gravity.RIGHT)) {
            // Gravity.RIGHT 始终右对齐，需要根据布局方向判断
            if (isRtl) Layout.Alignment.ALIGN_NORMAL else Layout.Alignment.ALIGN_OPPOSITE
        } else if (hasFlag(gravity, Gravity.START)) {
            // Gravity.START 等于 ALIGN_NORMAL，自动根据布局方向调整
            Layout.Alignment.ALIGN_NORMAL
        } else if (hasFlag(gravity, Gravity.END)) {
            // Gravity.END 等于 ALIGN_OPPOSITE，自动根据布局方向调整
            Layout.Alignment.ALIGN_OPPOSITE
        } else if (hasFlag(gravity, Gravity.CENTER) ||
            hasFlag(gravity, Gravity.CENTER_HORIZONTAL)
        ) {
            // 居中对齐，这里的对齐只能水平居中，而不支持垂直居中
            Layout.Alignment.ALIGN_CENTER
        } else {
            // 默认左对�?
            Layout.Alignment.ALIGN_NORMAL
        }

        // 坏消息：这种方式只支持水平方向文本重心设置，不支持垂直方向文本重心设�?
        // 好消息：在设置自定义 Span 的情况下，TextView 设置文本重心，目前只发现了水平重心设置失效，垂直重心设置仍然有效
        return alignment
    }

    /**
     * 检查整数是否包含某个标�?
     */
    protected fun hasFlag(i: Int, flag: Int): Boolean {
        return (i and flag) == flag
    }
}