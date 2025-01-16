@file:Suppress("unused")

package io.core.common.util.ext.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Picture
import android.graphics.Rect
import android.os.Build
import android.text.Html
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.EdgeEffect
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.record
import androidx.core.graphics.withTranslation
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import androidx.core.view.marginBottom
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import io.core.common.helper.canvasrecorder.CanvasRecorder
import io.core.common.helper.canvasrecorder.record
import io.core.common.util.ext.currentTimeMillis
import io.core.common.util.ext.inputMethodManager
import io.core.common.util.ext.layoutInflater
import io.core.common.util.log.printOnDebug
import java.lang.reflect.Field

private tailrec fun getCompatActivity(context: Context?): AppCompatActivity? {
    return when (context) {
        is AppCompatActivity -> context
        is androidx.appcompat.view.ContextThemeWrapper -> getCompatActivity(context.baseContext)
        is android.view.ContextThemeWrapper -> getCompatActivity(context.baseContext)
        else -> null
    }
}

val View.activity: AppCompatActivity?
    get() = getCompatActivity(context)


fun View.hideSoftInput() = run {
    inputMethodManager.hideSoftInputFromWindow(this.windowToken, 0)
}

fun EditText.showSoftInput() = run {
    requestFocus()
    inputMethodManager.showSoftInput(this, InputMethodManager.RESULT_SHOWN)
}

fun View.disableAutoFill() = run {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        this.importantForAutofill = IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
    }
}


fun RecyclerView.setEdgeEffectColor(@ColorInt color: Int) {
    edgeEffectFactory = object : RecyclerView.EdgeEffectFactory() {
        override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
            val edgeEffect = super.createEdgeEffect(view, direction)
            edgeEffect.color = color
            return edgeEffect
        }
    }
}

fun ViewPager.setEdgeEffectColor(@ColorInt color: Int) {
    try {
        val clazz = ViewPager::class.java
        for (name in arrayOf("mLeftEdge", "mRightEdge")) {
            val field = clazz.getDeclaredField(name)
            field.isAccessible = true
            val edge = field.get(this)
            (edge as EdgeEffect).color = color
        }
    } catch (ignored: Exception) {
    }
}

fun EditText.disableEdit() {
    keyListener = null
}

fun View.gone() {
    if (visibility != GONE) {
        visibility = GONE
    }
}

fun View.gone(gone: Boolean) {
    if (gone) {
        gone()
    } else {
        visibility = VISIBLE
    }
}

fun View.visible() {
    if (visibility != VISIBLE) {
        visibility = VISIBLE
    }
}

fun View.visible(visible: Boolean) {
    if (visible && visibility != VISIBLE) {
        visibility = VISIBLE
    } else if (!visible && visibility == VISIBLE) {
        visibility = INVISIBLE
    }
}

fun View.screenshot(bitmap: Bitmap? = null, canvas: Canvas? = null): Bitmap? {
    return if (width > 0 && height > 0) {
        val screenshot = if (bitmap != null && bitmap.width == width && bitmap.height == height) {
            bitmap.eraseColor(Color.TRANSPARENT)
            bitmap
        } else {
            bitmap?.recycle()
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
        val c = canvas ?: Canvas()
        c.setBitmap(screenshot)
        c.save()
        c.translate(-scrollX.toFloat(), -scrollY.toFloat())
        this.draw(c)
        c.restore()
        c.setBitmap(null)
        screenshot.prepareToDraw()
        screenshot
    } else {
        null
    }
}

fun View.screenshot(picture: Picture) {
    if (width > 0 && height > 0) {
        picture.record(width, height) {
            withTranslation(-scrollX.toFloat(), -scrollY.toFloat()) {
                draw(this)
            }
        }
    }
}

fun View.screenshot(canvasRecorder: CanvasRecorder) {
    if (width > 0 && height > 0) {
        canvasRecorder.record(width, height) {
            draw(this)
        }
    }
}

fun View.setPaddingBottom(bottom: Int) {
    setPadding(paddingLeft, paddingTop, paddingRight, bottom)
}

fun SeekBar.progressAdd(int: Int) {
    progress += int
}

fun RadioGroup.getIndexById(id: Int): Int {
    for (i in 0 until this.childCount) {
        if (id == get(i).id) {
            return i
        }
    }
    return 0
}

fun RadioGroup.getCheckedIndex(): Int {
    for (i in 0 until this.childCount) {
        if (checkedRadioButtonId == get(i).id) {
            return i
        }
    }
    return 0
}

fun RadioGroup.checkByIndex(index: Int) {
    check(get(index).id)
}

@SuppressLint("ObsoleteSdkInt")
fun TextView.setHtml(html: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        text = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
    } else {
        @Suppress("DEPRECATION")
        text = Html.fromHtml(html)
    }
}

fun TextView.setTextIfNotEqual(charSequence: CharSequence?) {
    if (text != charSequence) {
        text = charSequence
    }
}

@SuppressLint("RestrictedApi")
fun PopupMenu.show(x: Int, y: Int) {
    kotlin.runCatching {
        val field: Field = this.javaClass.getDeclaredField("mPopup")
        field.isAccessible = true
        (field.get(this) as MenuPopupHelper).show(x, y)
    }.onFailure {
        it.printOnDebug()
    }
}

fun View.shouldHideSoftInput(event: MotionEvent): Boolean {
    if (this is EditText) {
        val l = intArrayOf(0, 0)
        getLocationInWindow(l)
        val left = l[0]
        val top = l[1]
        val bottom = top + getHeight()
        val right = left + getWidth()
        return !(event.x > left && event.x < right && event.y > top && event.y < bottom)
    }
    return false
}

fun View.applyStatusBarPadding(withInitialPadding: Boolean = false) {
    val initialPadding = if (withInitialPadding) topPadding else 0
    ViewCompat.setOnApplyWindowInsetsListener(this) { _, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
        topPadding = initialPadding + insets.top
        windowInsets
    }
}

fun View.applyNavigationBarPadding(withInitialPadding: Boolean = false) {
    val initialPadding = if (withInitialPadding) bottomPadding else 0
    ViewCompat.setOnApplyWindowInsetsListener(this) { _, windowInsets ->
        bottomPadding = initialPadding + windowInsets.navigationBarHeight
        windowInsets
    }
}

fun View.applyNavigationBarMargin(withInitialMargin: Boolean = false) {
    val initialMargin = if (withInitialMargin) marginBottom else 0
    ViewCompat.setOnApplyWindowInsetsListener(this) { _, windowInsets ->
        val lp = layoutParams as ViewGroup.MarginLayoutParams
        lp.bottomMargin = initialMargin + windowInsets.navigationBarHeight
        layoutParams = lp
        windowInsets
    }
}

fun View.setBackgroundKeepPadding(@DrawableRes backgroundResId: Int) {
    val paddingLeft = paddingLeft
    val paddingTop = paddingTop
    val paddingRight = paddingRight
    val paddingBottom = paddingBottom
    setBackgroundResource(backgroundResId)
    setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
}

fun View.onVisibilityChange(
    viewGroups: List<ViewGroup> = emptyList(), // 会被插入 Fragment 的容器集合
    needScrollListener: Boolean = true,
    block: (view: View, isVisible: Boolean) -> Unit
) {
    val KEY_VISIBILITY = "KEY_VISIBILITY".hashCode()
    val KEY_HAS_LISTENER = "KEY_HAS_LISTENER".hashCode()
    // 若当前控件已监听可见性，则返回
    if (getTag(KEY_HAS_LISTENER) == true) return

    // 检测可见性
    val checkVisibility = {
        // 获取上一次可见性
        val lastVisibility = getTag(KEY_VISIBILITY) as? Boolean
        // 判断控件是否出现在屏幕中
        val isInScreen = this.isInScreen
        // 首次可见性变更
        if (lastVisibility == null) {
            if (isInScreen) {
                block(this, true)
                setTag(KEY_VISIBILITY, true)
            }
        }
        // 非首次可见性变更
        else if (lastVisibility != isInScreen) {
            block(this, isInScreen)
            setTag(KEY_VISIBILITY, isInScreen)
        }
    }

    // 全局重绘监听器
    class LayoutListener : ViewTreeObserver.OnGlobalLayoutListener {
        // 标记位用于区别是否是遮挡case
        var addedView: View? = null
        override fun onGlobalLayout() {
            // 遮挡 case
            if (addedView != null) {
                // 插入视图矩形区域
                val addedRect = Rect().also { addedView?.getGlobalVisibleRect(it) }
                // 当前视图矩形区域
                val rect = Rect().also { this@onVisibilityChange.getGlobalVisibleRect(it) }
                // 如果插入视图矩形区域包含当前视图矩形区域，则视为当前控件不可见
                if (addedRect.contains(rect)) {
                    block(this@onVisibilityChange, false)
                    setTag(KEY_VISIBILITY, false)
                } else {
                    block(this@onVisibilityChange, true)
                    setTag(KEY_VISIBILITY, true)
                }
            }
            // 非遮挡 case
            else {
                checkVisibility()
            }
        }
    }

    val layoutListener = LayoutListener()
    // 编辑容器监听其插入视图时机
    viewGroups.forEachIndexed { index, viewGroup ->
        viewGroup.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View?, child: View?) {
                // 当控件插入，则置标记位
                layoutListener.addedView = child
            }

            override fun onChildViewRemoved(parent: View?, child: View?) {
                // 当控件移除，则置标记位
                layoutListener.addedView = null
            }
        })
    }
    viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    // 全局滚动监听器
    var scrollListener: ViewTreeObserver.OnScrollChangedListener? = null
    if (needScrollListener) {
        scrollListener = ViewTreeObserver.OnScrollChangedListener { checkVisibility() }
        viewTreeObserver.addOnScrollChangedListener(scrollListener)
    }
    // 全局焦点变化监听器
    val focusChangeListener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
        val lastVisibility = getTag(KEY_VISIBILITY) as? Boolean
        val isInScreen = this.isInScreen
        if (hasFocus) {
            if (lastVisibility != isInScreen) {
                block(this, isInScreen)
                setTag(KEY_VISIBILITY, isInScreen)
            }
        } else {
            if (lastVisibility == true) {
                block(this, false)
                setTag(KEY_VISIBILITY, false)
            }
        }
    }
    viewTreeObserver.addOnWindowFocusChangeListener(focusChangeListener)
    // 为避免内存泄漏，当视图被移出的同时反注册监听器
    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
        }

        override fun onViewDetachedFromWindow(v: View) {
            v ?: return
            // 有时候 View detach 后，还会执行全局重绘，为此退后反注册
            post {
                try {
                    v.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
                } catch (_: java.lang.Exception) {
                    v.viewTreeObserver.removeGlobalOnLayoutListener(layoutListener)
                }
                v.viewTreeObserver.removeOnWindowFocusChangeListener(focusChangeListener)
                if (scrollListener != null) v.viewTreeObserver.removeOnScrollChangedListener(
                    scrollListener
                )
                viewGroups.forEach { it.setOnHierarchyChangeListener(null) }
            }
            removeOnAttachStateChangeListener(this)
        }
    })
    // 标记已设置监听器
    setTag(KEY_HAS_LISTENER, true)
}

val View.isInScreen: Boolean
    get() = ViewCompat.isAttachedToWindow(this) && visibility == View.VISIBLE && getLocalVisibleRect(
        Rect()
    )

// 显示 View
fun View.show() {
    visibility = View.VISIBLE
}

// 隐藏 View
fun View.hide() {
    visibility = View.GONE
}

// 完全隐藏 View（包括占位）
fun View.invisible() {
    visibility = View.INVISIBLE
}

// 设置点击事件
fun View.onClick(action: () -> Unit) {
    setOnClickListener { action() }
}

// 防止多次点击
// 防止重复点击（防止快速点击触发多次）
fun View.setDebouncedClickListener(debounceTime: Long = 500L, onClick: () -> Unit) {
    var lastClickTime = 0L
    setOnClickListener {
        val currentTime = currentTimeMillis
        if (currentTime - lastClickTime > debounceTime) {
            onClick()
            lastClickTime = currentTime
        }
    }
}


// 简单的淡入动画
fun View.fadeIn(duration: Long = 300) {
    alpha = 0f
    visibility = View.VISIBLE
    animate().alpha(1f).setDuration(duration).start()
}

// 简单的淡出动画
fun View.fadeOut(duration: Long = 300) {
    animate().alpha(0f).setDuration(duration).withEndAction {
        visibility = View.GONE
    }.start()
}

// 缩放动画
fun View.scaleUp(duration: Long = 300) {
    scaleX = 0f
    scaleY = 0f
    visibility = View.VISIBLE
    animate().scaleX(1f).scaleY(1f).setDuration(duration).start()
}

fun View.scaleDown(duration: Long = 300) {
    animate().scaleX(0f).scaleY(0f).setDuration(duration).withEndAction {
        visibility = View.GONE
    }.start()
}


// 获取 View 的宽度
fun View.getWidthCompat(): Int {
    return if (width == 0) {
        getGlobalVisibleRect(Rect())
        width
    } else {
        width
    }
}

// 获取 View 的高度
fun View.getHeightCompat(): Int {
    return if (height == 0) {
        getGlobalVisibleRect(Rect())
        height
    } else {
        height
    }
}

// 设置 View 的边距
fun View.setMargin(left: Int, top: Int, right: Int, bottom: Int) {
    val params = layoutParams as ViewGroup.MarginLayoutParams
    params.setMargins(left, top, right, bottom)
    layoutParams = params
}


// 设置是否可见
fun View.setVisible(isVisible: Boolean) {
    visibility = if (isVisible) View.VISIBLE else View.GONE
}

// 设置是否可用
fun View.setEnabled(enabled: Boolean) {
    isEnabled = enabled
}


// 水平居中
fun View.centerHorizontally(parent: ViewGroup) {
    val params = layoutParams as ViewGroup.MarginLayoutParams
    params.leftMargin = (parent.width - width) / 2
    layoutParams = params
}

// 垂直居中
fun View.centerVertically(parent: ViewGroup) {
    val params = layoutParams as ViewGroup.MarginLayoutParams
    params.topMargin = (parent.height - height) / 2
    layoutParams = params
}

// 设置按下时的颜色变化（按下和松开时的背景色变化）
fun View.setPressEffect(pressedColor: Int, defaultColor: Int) {
    setOnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> v.setBackgroundColor(pressedColor)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.setBackgroundColor(defaultColor)
        }
        false
    }
}

// 设置 View 的阴影
fun View.setShadow(radius: Float, dx: Float, dy: Float, color: Int) {
    elevation = radius
    translationZ = radius
}

// 动态修改 View 的宽高
fun View.setWidthAndHeight(width: Int, height: Int) {
    layoutParams.width = width
    layoutParams.height = height
    requestLayout()
}

// 旋转 View
fun View.rotate(degrees: Float) {
    animate().rotation(degrees).setDuration(300).start()
}

// 获取 View 的截图
fun View.takeScreenshot(): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    draw(canvas)
    return bitmap
}

// 检查 View 是否被点击
fun View.isClicked(event: MotionEvent): Boolean {
    val rect = Rect()
    getGlobalVisibleRect(rect)
    return rect.contains(event.rawX.toInt(), event.rawY.toInt())
}

