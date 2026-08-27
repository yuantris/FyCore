package io.core.engine.shape.other

import android.widget.TextView
import io.core.engine.shape.config.ITextViewAttribute

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/21 10:58
 * @description
 * @author Yuan
 */
class TextViewAttribute(private val mTextView: TextView) : ITextViewAttribute {
    override fun getLayoutDirection(): Int {
        return mTextView.layoutDirection;
    }

    override fun getTextGravity(): Int {
        return mTextView.gravity
    }

    override fun getPaddingLeft(): Int {
        return mTextView.paddingLeft
    }

    override fun getPaddingRight(): Int {
        return mTextView.paddingRight
    }
}