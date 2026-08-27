package io.core.engine.shape.config

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/21 10:52
 * @description
 * @author Yuan
 */
interface ITextViewAttribute {
    /**
     * 获取当前布局方向
     */
    fun getLayoutDirection(): Int

    /**
     * 获取当前文本重心
     */
    fun getTextGravity(): Int

    /**
     * 获取 TextView 左内间距
     */
    fun getPaddingLeft(): Int

    /**
     * 获取 TextView 右内间距
     */
    fun getPaddingRight(): Int
}