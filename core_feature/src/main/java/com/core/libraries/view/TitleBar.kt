package com.core.libraries.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.size
import com.core.libraries.R

class TitleBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val leftButtonContainer: FrameLayout
    private val leftButton: ImageView
    private val titleTextView: TextView
    private val rightContainer: LinearLayout

    private val rightContainerChildSize = 2 // 默认右侧容器最多包含两个子 View
    private val defaultVerticalPadding = dpToPx(6)
    private val buttonTouchPadding = dpToPx(8) // 按钮点击区域扩展

    private var onClickListener: OnTitleClickListener? = null
    private var defaultClickEffect = ClickEffect.Rectangle

    private var leftIcon: Int = 0
    private var rightIcon: Int = 0
    private var rightText: String = ""

    // 点击效果
    private enum class ClickEffect {
        Rectangle,
        RoundedRectangle,
        Circle
    }

    interface OnTitleClickListener {
        fun onBackClick() {}
        fun onRightXmlImgClick() {}
        fun onRightXmlTextClick() {}
    }


    init {
        setBackgroundColor(Color.WHITE)

        // 左侧按钮容器
        leftButtonContainer = FrameLayout(context).apply {
            layoutParams =
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    marginStart = dpToPx(8)
                }
        }
        addView(leftButtonContainer)

        // 左侧按钮
        leftButton = ImageView(context).apply {
            layoutParams =
                FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                    .apply {
                        gravity = Gravity.CENTER
                    }
            visibility = View.GONE
        }
        leftButtonContainer.addView(leftButton)

        // 扩大点击区域
        leftButtonContainer.setPadding(
            buttonTouchPadding,
            buttonTouchPadding,
            buttonTouchPadding,
            buttonTouchPadding
        )

        //setDefaultClickEffect(leftButtonContainer)
        //setRoundedClickEffect(6,leftButtonContainer)
        setClickEffect(leftButtonContainer)


        // 标题文本
        titleTextView = TextView(context).apply {
            layoutParams =
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER
                }
            textSize = 18f
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
        }
        addView(titleTextView)

        // 右侧容器
        rightContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams =
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.END or Gravity.CENTER_VERTICAL
                    marginEnd = dpToPx(8)
                }
        }
        addView(rightContainer)

        // 设置默认竖向 Padding
        setPadding(paddingLeft, defaultVerticalPadding, paddingRight, defaultVerticalPadding)

        // 解析 XML 属性
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.TitleBar)
            val title = typedArray.getString(R.styleable.TitleBar_titleText) ?: ""
            val titleColor = typedArray.getColor(
                R.styleable.TitleBar_titleColor,
                ContextCompat.getColor(context, R.color.black)
            )
            val titleSize =
                typedArray.getDimensionPixelSize(R.styleable.TitleBar_titleSize, spToPx(18f))
            val titleLeftAlign = typedArray.getBoolean(R.styleable.TitleBar_titleLeftAlign, false)
            val bgColor =
                typedArray.getColor(R.styleable.TitleBar_bgColor, Color.WHITE)
            leftIcon = typedArray.getResourceId(R.styleable.TitleBar_leftIcon, 0)
            rightIcon = typedArray.getResourceId(R.styleable.TitleBar_rightIcon, 0)
            rightText = typedArray.getString(R.styleable.TitleBar_rightText) ?: ""
            val verticalPadding = typedArray.getDimensionPixelSize(
                R.styleable.TitleBar_verticalPadding, defaultVerticalPadding
            )

            val clickEffect = typedArray.getInt(R.styleable.TitleBar_clickEffect, 0)
            when (clickEffect) {
                0 -> defaultClickEffect = ClickEffect.Rectangle
                1 -> defaultClickEffect = ClickEffect.RoundedRectangle
                2 -> defaultClickEffect = ClickEffect.Circle
            }

            titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, pxToSp(titleSize.toFloat()))
            titleTextView.setTextColor(titleColor)

            setBackgroundColor(bgColor)
            setTitle(title, alignLeft = titleLeftAlign)
            setPadding(paddingLeft, verticalPadding, paddingRight, verticalPadding)

            setLeftButton(
                if (leftIcon != 0) leftIcon
                else R.drawable.bar_arrows_left_black
            ) { onClickListener?.onBackClick() }

            typedArray.recycle()
        }

    }

    fun setOnTitleClickListener(listener: OnTitleClickListener?) {
        onClickListener = listener
        if (rightText.isNotBlank()) {
            addRightButtonText(
                rightText
            ) { onClickListener?.onRightXmlTextClick() }
        }
        if (rightIcon != 0) {
            addRightButtonImage(
                rightIcon
            ) { onClickListener?.onRightXmlImgClick() }
        }
        if (leftIcon != 0) {
            setLeftButton(
                leftIcon
            ) { onClickListener?.onBackClick() }
        }
    }

    // 设置标题文本
    fun setTitle(title: String, alignLeft: Boolean = false) {
        titleTextView.text = title
        post {
            val layoutParams = titleTextView.layoutParams as LayoutParams
            layoutParams.gravity =
                if (alignLeft) Gravity.START or Gravity.CENTER_VERTICAL else Gravity.CENTER
            layoutParams.marginStart = if (alignLeft) {
                leftButtonContainer.width + dpToPx(8 * 2) // 避免遮挡左侧按钮
            } else {
                0
            }
            titleTextView.layoutParams = layoutParams
        }

    }

    // 设置标题位置
    fun setTitleAlign(alignLeft: Boolean = false) {
        post {
            val layoutParams = titleTextView.layoutParams as LayoutParams
            layoutParams.gravity =
                if (alignLeft) Gravity.START or Gravity.CENTER_VERTICAL else Gravity.CENTER
            layoutParams.marginStart = if (alignLeft) {
                leftButtonContainer.width + dpToPx(8 * 2) // 避免遮挡左侧按钮
            } else {
                0
            }
            titleTextView.layoutParams = layoutParams
        }

    }

    // 设置标题尺寸
    fun setTitleSize(size: Int) {
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, pxToSp(size.toFloat()))
    }

    // 设置标题颜色
    fun setTitleColor(@ColorInt color: Int) {
        titleTextView.setTextColor(color)
    }

    // 设置标题字重
    fun setTitleStyle(style: Int) {
        titleTextView.setTypeface(null, style)
    }

    // 设置背景颜色
    override fun setBackgroundColor(@ColorInt color: Int) {
        super.setBackgroundColor(color)
    }

    // 设置左侧按钮
    fun setLeftButton(@DrawableRes iconRes: Int?, onClick: (() -> Unit)?) {
        leftButton.visibility = View.VISIBLE
        iconRes?.let {
            leftButton.setImageDrawable(ContextCompat.getDrawable(context, it))
        }
        setClickEffect(leftButtonContainer)
        leftButtonContainer.setOnClickListener {
            onClick?.invoke()
        }
    }

    // 添加右侧按钮（图片）
    fun addRightButtonImage(@DrawableRes iconRes: Int?, onClick: (() -> Unit)?) {
        //rightContainer.removeAllViews() // 确保不会重复添加图片
        val imageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(40),
                dpToPx(40)
            ).apply {
                marginStart = dpToPx(8)
            }
            setPadding(
                buttonTouchPadding,
                buttonTouchPadding,
                buttonTouchPadding,
                buttonTouchPadding
            )
            iconRes?.let {
                if (it != 0)
                    setImageDrawable(ContextCompat.getDrawable(context, iconRes))
            }
            onClick?.let { a ->
                // setClickEffect(this)
                // 右侧图标为点击缩放效果
                setOnClickListener {
                    it.animate()
                        .scaleX(0.9f)
                        .scaleY(0.9f)
                        .setDuration(150)
                        .withEndAction {
                            it.scaleX = 1f
                            it.scaleY = 1f
                            a.invoke()
                        }
                }
            }

        }

        if (rightContainer.size < rightContainerChildSize) {
            rightContainer.addView(imageView)
        } else throw RuntimeException("右侧按钮数量超出限制")

    }

    // 添加右侧按钮（文字）
    fun addRightButtonText(text: String, onClick: (() -> Unit)?) {
        val textView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = dpToPx(8)
            }
            this.text = text
            textSize = 16f
            setPadding(
                buttonTouchPadding,
                buttonTouchPadding,
                buttonTouchPadding,
                buttonTouchPadding
            )
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            onClick?.let { a ->
                setClickEffect(this)
                setOnClickListener {
                    a.invoke()
                }
            }
        }
        if (rightContainer.size < rightContainerChildSize) {
            rightContainer.addView(textView)
        } else throw RuntimeException("右侧按钮数量超出限制")
    }

    // 设置控件的点击效果
    private fun setClickEffect(view: View) {
        when (defaultClickEffect) {
            ClickEffect.Rectangle -> setDefaultClickEffect(view)
            ClickEffect.RoundedRectangle -> setRoundedClickEffect(6, view)
            ClickEffect.Circle -> setRoundedClickEffect(100, view)
        }
    }

    // 设置圆角点击效果
    private fun setRoundedClickEffect(angle: Int, view: View) {
        val rippleColor = ContextCompat.getColor(context, R.color.black5)  // 波纹效果颜色
        val cornerRadius = dpToPx(angle).toFloat()

        // 背景：透明色
        val backgroundDrawable = GradientDrawable().apply {
            setColor(Color.TRANSPARENT) // 设置完全透明
            setCornerRadius(cornerRadius)
        }

        // Mask：波纹的实际范围（设置为白色或浅色，但不会显示出来）
        val maskDrawable = GradientDrawable().apply {
            setColor(Color.WHITE) // mask 的颜色不会影响最终的 UI
            setCornerRadius(cornerRadius)
        }

        // RippleDrawable
        val rippleDrawable = RippleDrawable(
            ColorStateList.valueOf(rippleColor), // 波纹颜色
            backgroundDrawable, // 背景（透明）
            maskDrawable // 波纹作用范围
        )

        // 设置背景
        view.background = rippleDrawable
    }


    // 设置矩形点击效果
    private fun setDefaultClickEffect(view: View) {
        val drawable = StateListDrawable()
        drawable.addState(
            intArrayOf(android.R.attr.state_pressed),
            ColorDrawable(ContextCompat.getColor(context, R.color.black5))
        )
        drawable.addState(
            intArrayOf(android.R.attr.state_selected),
            ColorDrawable(ContextCompat.getColor(context, R.color.black5))
        )
        drawable.addState(
            intArrayOf(android.R.attr.state_focused),
            ColorDrawable(ContextCompat.getColor(context, R.color.black5))
        )
        drawable.addState(
            intArrayOf(),
            ColorDrawable(ContextCompat.getColor(context, R.color.transparent))
        )
        // 设置背景
        view.background = drawable
    }

    // 工具方法：dp 转 px
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    // 工具方法：sp 转 px
    private fun spToPx(sp: Float): Int {
        return (sp * resources.displayMetrics.scaledDensity).toInt()
    }

    // 工具方法：px 转 sp
    private fun pxToSp(px: Float): Float {
        return px / resources.displayMetrics.scaledDensity
    }
}
