package io.core.engine.shape.builder

import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.widget.TextView
import io.core.engine.shape.config.ITextColorStyleable
import io.core.engine.shape.config.ITextViewAttribute
import io.core.engine.shape.other.TextViewAttribute
import io.core.engine.shape.span.LinearGradientFontSpan
import io.core.engine.shape.span.MultiFontSpan
import io.core.engine.shape.span.StrokeFontSpan


/**
# ██████████
# █▄█████▄�?
# █▼▼▼▼▼
# �?
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载…�?
 * 2025/1/21 13:47
 * @description
 * @author Yuan
 */
class TextColorBuilder(textView: TextView, typedArray: TypedArray, styleable: ITextColorStyleable) {
    private val mTextView: TextView = textView
    private var mTextViewAttribute: ITextViewAttribute? = null

    private var mTextColor = typedArray.getColor(
        styleable.getTextColorStyleable(),
        textView.textColors.defaultColor
    )
    private var mTextPressedColor: Int? = null
    private var mTextCheckedColor: Int? = null
    private var mTextDisabledColor: Int? = null
    private var mTextFocusedColor: Int? = null
    private var mTextSelectedColor: Int? = null

    private var mTextGradientColors: IntArray? = null
    private var mTextGradientOrientation = 0

    private var mTextStrokeColor = 0
    private var mTextStrokeSize = 0

    init {
        if (typedArray.hasValue(styleable.getTextPressedColorStyleable())) {
            mTextPressedColor =
                typedArray.getColor(styleable.getTextPressedColorStyleable(), mTextColor)
        }
        if (styleable.getTextCheckedColorStyleable() > 0 && typedArray.hasValue(styleable.getTextCheckedColorStyleable())) {
            mTextCheckedColor =
                typedArray.getColor(styleable.getTextCheckedColorStyleable(), mTextColor)
        }
        if (typedArray.hasValue(styleable.getTextDisabledColorStyleable())) {
            mTextDisabledColor =
                typedArray.getColor(styleable.getTextDisabledColorStyleable(), mTextColor)
        }
        if (typedArray.hasValue(styleable.getTextFocusedColorStyleable())) {
            mTextFocusedColor =
                typedArray.getColor(styleable.getTextFocusedColorStyleable(), mTextColor)
        }
        if (typedArray.hasValue(styleable.getTextSelectedColorStyleable())) {
            mTextSelectedColor =
                typedArray.getColor(styleable.getTextSelectedColorStyleable(), mTextColor)
        }

        if (typedArray.hasValue(styleable.getTextStartColorStyleable()) && typedArray.hasValue(
                styleable.getTextEndColorStyleable()
            )
        ) {
            mTextGradientColors =
                if (typedArray.hasValue(styleable.getTextCenterColorStyleable())) {
                    intArrayOf(
                        typedArray.getColor(styleable.getTextStartColorStyleable(), mTextColor),
                        typedArray.getColor(styleable.getTextCenterColorStyleable(), mTextColor),
                        typedArray.getColor(styleable.getTextEndColorStyleable(), mTextColor)
                    )
                } else {
                    intArrayOf(
                        typedArray.getColor(styleable.getTextStartColorStyleable(), mTextColor),
                        typedArray.getColor(styleable.getTextEndColorStyleable(), mTextColor)
                    )
                }
        }

        mTextGradientOrientation = typedArray.getColor(
            styleable.getTextGradientOrientationStyleable(),
            LinearGradientFontSpan.GRADIENT_ORIENTATION_HORIZONTAL
        );

        if (typedArray.hasValue(styleable.getTextStrokeColorStyleable())) {
            mTextStrokeColor =
                typedArray.getColor(styleable.getTextStrokeColorStyleable(), Color.TRANSPARENT);
        }

        if (typedArray.hasValue(styleable.getTextStrokeSizeStyleable())) {
            mTextStrokeSize =
                typedArray.getDimensionPixelSize(styleable.getTextStrokeSizeStyleable(), 0);
        }

        mTextViewAttribute = TextViewAttribute(mTextView);
    }

    fun setTextColor(color: Int): TextColorBuilder {
        mTextColor = color
        return this
    }

    fun getTextColor(): Int {
        return mTextColor
    }

    fun setTextPressedColor(color: Int): TextColorBuilder {
        mTextPressedColor = color
        return this
    }

    fun getTextPressedColor(): Int? {
        return mTextPressedColor
    }

    fun setTextCheckedColor(color: Int): TextColorBuilder {
        mTextCheckedColor = color
        return this
    }

    fun getTextCheckedColor(): Int? {
        return mTextCheckedColor
    }

    fun setTextDisabledColor(color: Int): TextColorBuilder {
        mTextDisabledColor = color
        return this
    }

    fun getTextDisabledColor(): Int? {
        return mTextDisabledColor
    }

    fun setTextFocusedColor(color: Int): TextColorBuilder {
        mTextFocusedColor = color
        return this
    }

    fun getTextFocusedColor(): Int? {
        return mTextFocusedColor
    }

    fun setTextSelectedColor(color: Int): TextColorBuilder {
        mTextSelectedColor = color
        return this
    }

    fun getTextSelectedColor(): Int? {
        return mTextSelectedColor
    }


    fun setTextGradientColors(startColor: Int, endColor: Int): TextColorBuilder {
        return setTextGradientColors(intArrayOf(startColor, endColor))
    }

    fun setTextGradientColors(startColor: Int, centerColor: Int, endColor: Int): TextColorBuilder {
        return setTextGradientColors(intArrayOf(startColor, centerColor, endColor))
    }

    fun setTextGradientColors(colors: IntArray): TextColorBuilder {
        mTextGradientColors = colors
        return this
    }

    fun getTextGradientColors(): IntArray? {
        return mTextGradientColors
    }

    fun isTextGradientColorsEnable(): Boolean {
        return mTextGradientColors?.isNotEmpty() == true
    }

    fun setTextGradientOrientation(orientation: Int): TextColorBuilder {
        mTextGradientOrientation = orientation
        return this
    }

    fun getTextGradientOrientation(): Int {
        return mTextGradientOrientation
    }

    fun setTextStrokeColor(color: Int): TextColorBuilder {
        mTextStrokeColor = color
        return this
    }

    fun setTextStrokeSize(size: Int): TextColorBuilder {
        mTextStrokeSize = size
        return this
    }

    fun getTextStrokeColor(): Int {
        return mTextStrokeColor
    }

    fun getTextStrokeSize(): Int {
        return mTextStrokeSize
    }

    fun isTextStrokeColorEnable(): Boolean {
        return mTextStrokeColor != Color.TRANSPARENT && mTextStrokeSize > 0
    }

    fun clearTextSpannable() {
        mTextStrokeColor = Color.TRANSPARENT
        mTextStrokeSize = 0
        if (!isTextGradientColorsEnable()) {
            mTextView.setTextColor(mTextColor)
        }
        mTextView.text = mTextView.text.toString()
    }


    fun buildTextSpannable(text: CharSequence?): SpannableStringBuilder {
        val builder = SpannableStringBuilder(text)

        var linearGradientFontSpan: LinearGradientFontSpan? = null
        var strokeFontSpan: StrokeFontSpan? = null

        if (isTextGradientColorsEnable()) {
            linearGradientFontSpan = LinearGradientFontSpan(mTextViewAttribute!!)
                .setTextGradientColor(mTextGradientColors)
                .setTextGradientOrientation(mTextGradientOrientation)
                .setTextGradientPositions(null)
        }
        if (isTextStrokeColorEnable()) {
            strokeFontSpan = StrokeFontSpan(mTextViewAttribute!!)
                .setTextStrokeColor(mTextStrokeColor)
                .setTextStrokeSize(mTextStrokeSize)
        }

        if (linearGradientFontSpan != null && strokeFontSpan != null) {
            val multiFontSpan =
                MultiFontSpan(mTextViewAttribute!!, strokeFontSpan, linearGradientFontSpan)
            builder.setSpan(multiFontSpan, 0, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        } else if (linearGradientFontSpan != null) {
            builder.setSpan(
                linearGradientFontSpan,
                0,
                builder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        } else if (strokeFontSpan != null) {
            strokeFontSpan.setTextSolidColor(mTextColor)
            builder.setSpan(strokeFontSpan, 0, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        return builder
    }

    fun buildColorState(): ColorStateList {
        if (mTextPressedColor == null
            && mTextCheckedColor == null
            && mTextDisabledColor == null
            && mTextFocusedColor == null
            && mTextSelectedColor == null
        ) {
            return ColorStateList.valueOf(mTextColor)
        }

        val maxSize = 6
        var arraySize = 0
        val statesTemp = arrayOfNulls<IntArray>(maxSize)
        val colorsTemp = IntArray(maxSize)

        if (mTextPressedColor != null) {
            statesTemp[arraySize] = intArrayOf(android.R.attr.state_pressed)
            colorsTemp[arraySize] = mTextPressedColor!!
            arraySize++
        }
        if (mTextCheckedColor != null) {
            statesTemp[arraySize] = intArrayOf(android.R.attr.state_checked)
            colorsTemp[arraySize] = mTextCheckedColor!!
            arraySize++
        }
        if (mTextDisabledColor != null) {
            statesTemp[arraySize] = intArrayOf(-android.R.attr.state_enabled)
            colorsTemp[arraySize] = mTextDisabledColor!!
            arraySize++
        }
        if (mTextFocusedColor != null) {
            statesTemp[arraySize] = intArrayOf(android.R.attr.state_focused)
            colorsTemp[arraySize] = mTextFocusedColor!!
            arraySize++
        }
        if (mTextSelectedColor != null) {
            statesTemp[arraySize] = intArrayOf(android.R.attr.state_selected)
            colorsTemp[arraySize] = mTextSelectedColor!!
            arraySize++
        }

        statesTemp[arraySize] = intArrayOf()
        colorsTemp[arraySize] = mTextColor
        arraySize++

        val states: Array<IntArray?>
        val colors: IntArray
        if (arraySize == maxSize) {
            states = statesTemp
            colors = colorsTemp
        } else {
            states = arrayOfNulls(arraySize)
            colors = IntArray(arraySize)
            // 对数组进行拷�?
            System.arraycopy(statesTemp, 0, states, 0, arraySize)
            System.arraycopy(colorsTemp, 0, colors, 0, arraySize)
        }
        return ColorStateList(states, colors)
    }

    fun intoTextColor() {
        mTextView.setTextColor(buildColorState())
        if (isTextGradientColorsEnable() || isTextStrokeColorEnable()) {
            mTextView.text = buildTextSpannable(mTextView.text)
        }
    }
}