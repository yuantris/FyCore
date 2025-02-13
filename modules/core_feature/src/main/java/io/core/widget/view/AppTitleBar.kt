package io.core.widget.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Menu
import android.view.View
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.alpha
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import com.google.android.material.appbar.AppBarLayout
import io.core.R
import io.core.common.util.ext.cool.dpToPx
import io.core.common.util.ext.ui.activity
import io.core.common.util.ext.ui.bottomPadding
import io.core.common.util.ext.ui.topPadding

@Suppress("unused", "MemberVisibilityCanBePrivate")
class AppTitleBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppBarLayout(context, attrs) {

    val toolbar: Toolbar
    val menu: Menu
        get() = toolbar.menu

    var title: CharSequence?
        get() = toolbar.title
        set(title) {
            if (toolbar.title != title) {
                toolbar.title = title
            }
        }

    var subtitle: CharSequence?
        get() = toolbar.subtitle
        set(subtitle) {
            if (toolbar.subtitle != subtitle) {
                toolbar.subtitle = subtitle
            }
        }

    private val displayHomeAsUp: Boolean
    private val navigationIconTint: ColorStateList?
    private val navigationIconTintMode: Int
    private val fitStatusBar: Boolean
    private val fitNavigationBar: Boolean
    private val attachToActivity: Boolean

    init {
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.AppTitleBar,
            R.attr.AppTitleBarStyle, 0
        )
        navigationIconTint = a.getColorStateList(R.styleable.AppTitleBar_navigationIconTint)
//        navigationIconTintMode = a.getInt(R.styleable.AppTitleBar_navigationIconTintMode, 9)
        navigationIconTintMode=9
        attachToActivity = a.getBoolean(R.styleable.AppTitleBar_attachToActivity, true)
        displayHomeAsUp = a.getBoolean(R.styleable.AppTitleBar_displayHomeAsUp, true)
        fitStatusBar = a.getBoolean(R.styleable.AppTitleBar_fitStatusBar, true)
        fitNavigationBar = a.getBoolean(R.styleable.AppTitleBar_fitNavigationBar, false)

        val navigationIcon = a.getDrawable(R.styleable.AppTitleBar_navigationIcon)
        val navigationContentDescription =
            a.getText(R.styleable.AppTitleBar_navigationContentDescription)
        val titleText = a.getString(R.styleable.AppTitleBar_title)
        val subtitleText = a.getString(R.styleable.AppTitleBar_subtitle)

        when (a.getInt(R.styleable.AppTitleBar_themeMode, 0)) {
            1 -> inflate(context, R.layout.view_title_bar_dark, this)
            else -> inflate(context, R.layout.view_title_bar, this)
        }
        toolbar = findViewById(R.id.toolbar)

        toolbar.apply {
            navigationIcon?.let {
                this.navigationIcon = it
                this.navigationContentDescription = navigationContentDescription
            }

            if (a.hasValue(R.styleable.AppTitleBar_titleTextAppearance)) {
                this.setTitleTextAppearance(
                    context,
                    a.getResourceId(R.styleable.AppTitleBar_titleTextAppearance, 0)
                )
            }

            if (a.hasValue(R.styleable.AppTitleBar_titleTextColor)) {
                this.setTitleTextColor(a.getColor(R.styleable.AppTitleBar_titleTextColor, -0x1))
            }

            if (a.hasValue(R.styleable.AppTitleBar_subtitleTextAppearance)) {
                this.setSubtitleTextAppearance(
                    context,
                    a.getResourceId(R.styleable.AppTitleBar_subtitleTextAppearance, 0)
                )
            }

            if (a.hasValue(R.styleable.AppTitleBar_subtitleTextColor)) {
                this.setSubtitleTextColor(
                    a.getColor(
                        R.styleable.AppTitleBar_subtitleTextColor,
                        -0x1
                    )
                )
            }


            if (a.hasValue(R.styleable.AppTitleBar_contentInsetLeft)
                || a.hasValue(R.styleable.AppTitleBar_contentInsetRight)
            ) {
                this.setContentInsetsAbsolute(
                    a.getDimensionPixelSize(R.styleable.AppTitleBar_contentInsetLeft, 0),
                    a.getDimensionPixelSize(R.styleable.AppTitleBar_contentInsetRight, 0)
                )
            }

            if (a.hasValue(R.styleable.AppTitleBar_contentInsetStart)
                || a.hasValue(R.styleable.AppTitleBar_contentInsetEnd)
            ) {
                this.setContentInsetsRelative(
                    a.getDimensionPixelSize(R.styleable.AppTitleBar_contentInsetStart, 0),
                    a.getDimensionPixelSize(R.styleable.AppTitleBar_contentInsetEnd, 0)
                )
            }

            if (a.hasValue(R.styleable.AppTitleBar_contentInsetStartWithNavigation)) {
                this.contentInsetStartWithNavigation = a.getDimensionPixelOffset(
                    R.styleable.AppTitleBar_contentInsetStartWithNavigation, 0
                )
            }

            if (a.hasValue(R.styleable.AppTitleBar_contentInsetEndWithActions)) {
                this.contentInsetEndWithActions = a.getDimensionPixelOffset(
                    R.styleable.AppTitleBar_contentInsetEndWithActions, 0
                )
            }

            if (!titleText.isNullOrBlank()) {
                this.title = titleText
            }

            if (!subtitleText.isNullOrBlank()) {
                this.subtitle = subtitleText
            }

            if (a.hasValue(R.styleable.AppTitleBar_contentLayout)) {
                inflate(context, a.getResourceId(R.styleable.AppTitleBar_contentLayout, 0), this)
            }
        }

        if (!isInEditMode) {
//            if (fitStatusBar) {
//                setPadding(paddingLeft, context.statusBarHeight, paddingRight, paddingBottom)
//            }
//
//            if (fitNavigationBar) {
//                setPadding(paddingLeft, paddingTop, paddingRight, context.navigationBarHeight)
//            }

            if (fitStatusBar || fitNavigationBar) {
                ViewCompat.setOnApplyWindowInsetsListener(this) { _, windowInsets ->
                    val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                    if (fitStatusBar) {
                        topPadding = insets.top
                    }
                    if (fitNavigationBar) {
                        bottomPadding = insets.bottom
                    }
                    windowInsets
                }
            }


            setBackgroundColor(context.getColor(R.color.color_white))


            stateListAnimator = null
            elevation = 4f.dpToPx()
        }
        a.recycle()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attachToActivity()
    }

    fun setNavigationOnClickListener(clickListener: ((View) -> Unit)) {
        toolbar.setNavigationOnClickListener(clickListener)
    }

    fun setTitle(titleId: Int) {
        toolbar.setTitle(titleId)
    }

    fun setSubTitle(subtitleId: Int) {
        toolbar.setSubtitle(subtitleId)
    }

    fun setTitleTextColor(@ColorInt color: Int) {
        toolbar.setTitleTextColor(color)
    }

    fun setTitleTextAppearance(@StyleRes resId: Int) {
        toolbar.setTitleTextAppearance(context, resId)
    }

    fun setSubTitleTextColor(@ColorInt color: Int) {
        toolbar.setSubtitleTextColor(color)
    }

    fun setSubTitleTextAppearance(@StyleRes resId: Int) {
        toolbar.setSubtitleTextAppearance(context, resId)
    }

    fun setTextColor(@ColorInt color: Int) {
        setTitleTextColor(color)
        setSubTitleTextColor(color)
    }

    fun setColorFilter(@ColorInt color: Int) {
        val colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        toolbar.children.firstOrNull { it is ImageView }?.background?.colorFilter = colorFilter
        toolbar.navigationIcon?.colorFilter = colorFilter
        toolbar.overflowIcon?.colorFilter = colorFilter
        toolbar.menu.children.forEach {
            it.icon?.colorFilter = colorFilter
        }
    }

    override fun setBackgroundColor(color: Int) {
        if (color.alpha < 255) {
            //这里不能改为0f,改为0f在横屏模式下文字和图标颜色会变
            elevation = 0.1f
        }
        super.setBackgroundColor(color)
    }

    override fun setBackground(background: Drawable?) {
        if (background is ColorDrawable) {
            if (background.alpha < 255) {
                //这里不能改为0f,改为0f在横屏模式下文字和图标颜色会变
                elevation = 0.1f
            }
        }
        super.setBackground(background)
    }

    fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, fullScreen: Boolean) {
//        if (fitStatusBar) {
//            val topPadding = if (!isInMultiWindowMode && fullScreen) context.statusBarHeight else 0
//            setPadding(paddingLeft, topPadding, paddingRight, paddingBottom)
//        }
    }


    private fun attachToActivity() {
        if (attachToActivity) {
            activity?.let {
                it.setSupportActionBar(toolbar)
                it.supportActionBar?.setDisplayHomeAsUpEnabled(displayHomeAsUp)
            }
        }
    }

}