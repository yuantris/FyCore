package com.core.libraries.widget.layout

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.BarUtils.getStatusBarHeight
import com.core.libraries.R
import com.core.libraries.common.util.ext.cool.dp2px
import com.core.libraries.common.util.ext.ui.getActivity
import com.core.libraries.common.util.log.logD
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout

class CollapsingHeaderLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CoordinatorLayout(context, attrs, defStyleAttr) {

    private var appBarLayout: AppBarLayout
    private val collapsingToolbarLayout: CollapsingToolbarLayout
    private val toolbar: Toolbar
    private val headerImage: ImageView
    private val contentContainer: FrameLayout

    // 自定义属性
    var headerHeight: Int = 300.dp2px(context)
    var headerTitle: String? = null
    var headerImageResId: Int = 0
    var titleTextColor: Int = Color.BLACK
    var scrimColor: Int = ContextCompat.getColor(context, android.R.color.holo_blue_dark)
    var scrimAnimationDuration: Long = 300L
    var isBackShow: Boolean = true

    // 回调监听
    private var onScrollProgressListener: ((progress: Float) -> Unit)? = null
    private var onCollapseStateChangeListener: ((isCollapsed: Boolean) -> Unit)? = null

    init {
        // 初始化视图
        appBarLayout = AppBarLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, headerHeight)
        }

        collapsingToolbarLayout = CollapsingToolbarLayout(context).apply {
            layoutParams = AppBarLayout.LayoutParams(
                AppBarLayout.LayoutParams.MATCH_PARENT,
                AppBarLayout.LayoutParams.MATCH_PARENT
            ).apply {
                scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                        AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
            }
            setContentScrimColor(scrimColor)
            scrimAnimationDuration = this@CollapsingHeaderLayout.scrimAnimationDuration
        }

        headerImage = ImageView(context).apply {
            layoutParams = CollapsingToolbarLayout.LayoutParams(
                CollapsingToolbarLayout.LayoutParams.MATCH_PARENT,
                CollapsingToolbarLayout.LayoutParams.MATCH_PARENT
            ).apply {
                collapseMode = CollapsingToolbarLayout.LayoutParams.COLLAPSE_MODE_PARALLAX
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        toolbar = Toolbar(context).apply {
            layoutParams = CollapsingToolbarLayout.LayoutParams(
                CollapsingToolbarLayout.LayoutParams.MATCH_PARENT,
                context.theme.obtainStyledAttributes(
                    intArrayOf(android.R.attr.actionBarSize)
                ).getDimension(0, 0f).toInt() + getStatusBarHeight()
            ).apply {
                collapseMode = CollapsingToolbarLayout.LayoutParams.COLLAPSE_MODE_PIN
            }
            setTitleTextColor(titleTextColor)
        }


        collapsingToolbarLayout.addView(headerImage)
        collapsingToolbarLayout.addView(toolbar)
        appBarLayout.addView(collapsingToolbarLayout)

        contentContainer = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            (layoutParams as LayoutParams).behavior = AppBarLayout.ScrollingViewBehavior()
        }

        addView(appBarLayout)
        addView(contentContainer)

        initAttributes(context, attrs)
        setupBackButton()
        applyStatusBarPadding()
        setupAppBarLayoutCallbacks()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        // 将所有子视图移动到 contentContainer 中
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child != appBarLayout && child != contentContainer) {
                removeView(child)
                contentContainer.addView(child)
            }
        }
    }

    fun getContentContainer(): FrameLayout = contentContainer

    private fun initAttributes(context: Context, attrs: AttributeSet?) {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.CollapsingHeaderLayout)

            // 解析自定义属性
            headerHeight = typedArray.getDimensionPixelSize(
                R.styleable.CollapsingHeaderLayout_headerHeight,
                headerHeight
            )
            headerTitle = typedArray.getString(R.styleable.CollapsingHeaderLayout_headerTitle)
            headerImageResId = typedArray.getResourceId(
                R.styleable.CollapsingHeaderLayout_headerImageSrc,
                headerImageResId
            )
            titleTextColor = typedArray.getColor(
                R.styleable.CollapsingHeaderLayout_headerTitleColor,
                titleTextColor
            )
            scrimColor = typedArray.getColor(
                R.styleable.CollapsingHeaderLayout_scrimColor,
                scrimColor
            )
            scrimAnimationDuration = typedArray.getInt(
                R.styleable.CollapsingHeaderLayout_scrimAnimationDuration,
                scrimAnimationDuration.toInt()
            ).toLong()

            isBackShow = typedArray.getBoolean(R.styleable.CollapsingHeaderLayout_isBackShow, true)

            typedArray.recycle()
        }

        updateAppBarLayout()
        setupBackButton()
        collapsingToolbarLayout.title = headerTitle
        collapsingToolbarLayout.setCollapsedTitleTextColor(titleTextColor)
        collapsingToolbarLayout.setExpandedTitleColor(titleTextColor)
        headerImage.setImageResource(headerImageResId)
        collapsingToolbarLayout.setContentScrimColor(scrimColor)
        collapsingToolbarLayout.scrimAnimationDuration = scrimAnimationDuration
        toolbar.setTitleTextColor(titleTextColor)
    }

    private fun setupBackButton() {
        if (isBackShow) {
            toolbar.setNavigationIcon(R.drawable.bar_arrows_left_black)
            toolbar.setNavigationOnClickListener {
                "我已点击".logD()
                context.getActivity()?.finish()
            }
        } else {
            toolbar.setNavigationIcon(null)
        }
    }

    private fun updateAppBarLayout() {
        appBarLayout.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            headerHeight
        )
    }

    private fun applyStatusBarPadding() {
        val statusBarHeight = getStatusBarHeight()
        toolbar.setPadding(
            toolbar.paddingLeft,
            statusBarHeight,
            toolbar.paddingRight,
            toolbar.paddingBottom
        )
    }


    private fun setupAppBarLayoutCallbacks() {
        appBarLayout.addOnOffsetChangedListener { _, verticalOffset ->
            val totalScrollRange = appBarLayout.totalScrollRange
            val progress = -verticalOffset / totalScrollRange.toFloat()
            onScrollProgressListener?.invoke(progress)

            if (progress == 1f) {
                onCollapseStateChangeListener?.invoke(true)
            } else if (progress == 0f) {
                onCollapseStateChangeListener?.invoke(false)
            }
        }
    }

    // 动态添加子视图
    fun addContentView(view: View) {
        contentContainer.addView(view)
    }

    // 动态移除子视图
    fun removeContentView(view: View) {
        contentContainer.removeView(view)
    }

    // 设置滚动进度监听器
    fun setOnScrollProgressListener(listener: (progress: Float) -> Unit) {
        onScrollProgressListener = listener
    }

    // 设置折叠状态监听器
    fun setOnCollapseStateChangeListener(listener: (isCollapsed: Boolean) -> Unit) {
        onCollapseStateChangeListener = listener
    }
}
