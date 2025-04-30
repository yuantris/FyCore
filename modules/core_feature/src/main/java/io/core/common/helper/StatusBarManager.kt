package io.core.common.helper

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowInsetsController
import androidx.core.view.WindowCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import io.core.appCtx
import io.core.common.util.extensions.ui.statusBarHeight
import io.core.common.util.tools.isAndroid6Plus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 状态栏颜色管理
 */
class StatusBarManager private constructor(private val activity: Activity) : LifecycleObserver {

    // 颜色缓存
    private val colorCache = mutableMapOf<Int, Int>()

    // 防止重复更新
    private val isUpdating = AtomicBoolean(false)

    // 当前状态栏样式
    @Volatile
    private var currentLightStatusBar: Boolean? = null

    // 节流时间
    private var throttleTimeMs: Long = 100
    private var lastUpdateTime: Long = 0

    // 接管启用透明状态栏
    private var enableTransparentStatusBar: Boolean = false

    // 节流控制相关CoroutineScope
    private val coroutineScope by lazy {
        (activity as? LifecycleOwner)?.lifecycleScope ?: CoroutineScope(Dispatchers.Main)
    }
    private var updateJob: Job? = null


    /**
     * 初始化透明状态栏
     */
    init {
        if (enableTransparentStatusBar) {
            makeStatusBarTransparent()
        }
        if (activity is LifecycleOwner) {
            activity.lifecycle.addObserver(this)
        }
    }

    /**
     * 设置节流时间（毫秒）
     */
    fun setThrottleTime(timeMs: Long): StatusBarManager {
        this.throttleTimeMs = timeMs
        return this
    }

    /**
     * 设置是否接管启用透明状态栏
     */
    fun setEnableTransparentStatusBar(enable: Boolean): StatusBarManager {
        this.enableTransparentStatusBar = enable
        if (enable) {
            makeStatusBarTransparent()
        }
        return this
    }

    /**
     * 配置ViewPager2的状态栏颜色自动切换
     */
    fun setupWithViewPager(viewPager: ViewPager2, fragments: List<Fragment>) {
        val callback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateForFragment(fragments.getOrNull(position))
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                handlePageScrolled(fragments, position, positionOffset)
            }
        }
        viewPager.registerOnPageChangeCallback(callback)
        pageChangeCallbacks[viewPager] = callback
    }

    /**
     * 配置ViewPager的状态栏颜色自动切换
     */
    fun setupWithViewPager(viewPager: ViewPager, fragments: List<Fragment>) {
        val listener = object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                handlePageScrolled(fragments, position, positionOffset)
            }

            override fun onPageSelected(position: Int) {
                updateForFragment(fragments.getOrNull(position))
            }

            override fun onPageScrollStateChanged(state: Int) {}
        }
        viewPager.addOnPageChangeListener(listener)
        pageChangeListeners[viewPager] = listener
    }

    /**
     * 配置滚动视图的状态栏颜色自动切换
     * @param scrollView 滚动视图
     */
    @JvmOverloads
    fun setupWithScrollView(
        scrollView: NestedScrollView,
        scrollStopDelay: Long = 300
    ) {
        var scrollRunnable: Runnable? = null
        val listener = NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
            // 移除旧任务
            scrollRunnable?.let { v.removeCallbacks(it) }

            // 实时采样（节流控制）
            if (!shouldThrottle()) {
                updateStatusBarColorFromScroll(scrollView, scrollY)
            }

            // 滚动停止后最终采样
            scrollRunnable = Runnable {
                updateStatusBarColorFromScroll(scrollView, scrollY)
            }.also {
                scrollView.postDelayed(it, scrollStopDelay)
            }
        }

        scrollView.setOnScrollChangeListener(listener)
    }


    private fun updateStatusBarColorFromScroll(scrollView: NestedScrollView, scrollY: Int) {
        statusBarRect.set(0, 0, scrollView.width, statusBarHeight)
        scrollView.getGlobalVisibleRect(viewVisibleRect)

        if (statusBarRect.intersect(viewVisibleRect)) {
            val centerY = statusBarRect.centerY() - scrollY
            val color = getStatusBarColorFromView(scrollView, centerY.coerceAtLeast(0))
            updateStatusBarAppearance(color)
        } else {
            updateStatusBarAppearance(Color.WHITE) // 默认回退
        }
    }

    /**
     * 配置单个View的状态栏颜色自动切换
     */
    @JvmOverloads
    fun setupWithView(view: View, threshold: Int = 100) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            if (shouldThrottle()) return@OnGlobalLayoutListener
            val color = getStatusBarColorFromView(view, threshold)
            updateStatusBarAppearance(color)
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        layoutListeners[view] = listener
    }

    /**
     * 配置当前Activity的状态栏颜色自动切换
     */
    @JvmOverloads
    fun setupWithActivity(threshold: Int = 100) {
        val decorView = activity.window.decorView
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            if (shouldThrottle()) return@OnGlobalLayoutListener

            val color = getStatusBarColorFromView(activity.window.decorView, threshold)
            updateStatusBarAppearance(color)
        }
        decorView.viewTreeObserver.addOnGlobalLayoutListener(listener)
        decorViewListeners[decorView] = listener
    }

    /**
     * 手动更新状态栏颜色
     */
    fun updateStatusBarManually(isLight: Boolean) {
        currentLightStatusBar = isLight
        updateStatusBarAppearance(if (isLight) Color.WHITE else Color.BLACK,false)
    }

    /**
     * 主动根据当前页面更新状态栏颜色
     * 适用于页面内容已发生改变但布局未触发更新的场景
     */
    fun updateFromCurrentPage() {
        // 取消之前的任务
        updateJob?.cancel()
        updateJob = coroutineScope.launch {
            // 添加防抖延迟
            delay(50)
            // 获取Activity顶层视图
            val decorView = activity.window.decorView
            // 计算状态栏中心点Y坐标
            val centerY = (statusBarHeight / 2).coerceAtLeast(0)
            // 获取状态栏区域颜色
            val color = getStatusBarColorFromView(decorView, centerY)
            // 更新状态栏外观（带节流控制）
            if (!shouldThrottle()) {
                updateStatusBarAppearance(color, false)
            }
        }
    }

    /**
     * 获取系统当前状态栏文字是否为浅色模式（实时读取系统设置）
     * @return Boolean 当前系统实际状态栏文字是否为浅色模式
     */
    fun isStatusBarLight(): Boolean {
        return if (isAndroid6Plus) {
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                .isAppearanceLightStatusBars
        } else {
            // Android 6.0以下无法获取系统设置，返回当前维护状态
            currentLightStatusBar ?: false
        }
    }

    /**
     * 清除颜色缓存
     */
    fun clearCache() {
        colorCache.clear()
    }

    /**
     * 移除所有监听器
     * 只要Activity实现了 LifecycleOwner 接口(如AppCompatActivity)，就会在销毁时自动调用 cleanup()，无需外部手动调用。
     * 注意：如果Activity没有实现 LifecycleOwner ，仍然需要手动调用 cleanup()。
     */
    fun cleanup() {
        // 取消协程任务
        updateJob?.cancel()
        updateJob = null

        // 单独处理DecorView监听器
        decorViewListeners.forEach { (view, listener) ->
            if (view.isAttachedToWindow) {
                view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
            }
        }
        decorViewListeners.clear()

        // 移除全局布局监听器
        synchronized(layoutListeners) {
            layoutListeners.forEach { (view, listener) ->
                if (view.isAttachedToWindow) {
                    view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
                }
            }
            layoutListeners.clear()
        }

        // 移除滚动监听器
        synchronized(scrollListeners) {
            scrollListeners.forEach { (view, listener) ->
                if (view.isAttachedToWindow) {
                    view.viewTreeObserver.removeOnScrollChangedListener(listener)
                }
            }
            scrollListeners.clear()
        }

        // 移除ViewPager2回调
        synchronized(pageChangeCallbacks) {
            pageChangeCallbacks.forEach { (viewPager, callback) ->
                viewPager.unregisterOnPageChangeCallback(callback)
            }
            pageChangeCallbacks.clear()
        }

        // 移除ViewPager监听器
        synchronized(pageChangeListeners) {
            pageChangeListeners.forEach { (viewPager, listener) ->
                viewPager.removeOnPageChangeListener(listener)
            }
            pageChangeListeners.clear()
        }

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onDestroy() {
        cleanup()
        instances.remove(activity)
    }


    private fun makeStatusBarTransparent() {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        activity.window.statusBarColor = Color.TRANSPARENT
    }

    private fun handlePageScrolled(
        fragments: List<Fragment>,
        position: Int,
        positionOffset: Float
    ) {
        if (shouldThrottle()) return

        val currentFragment = fragments.getOrNull(position)
        val nextFragment = if (positionOffset > 0.5f) {
            fragments.getOrNull(position + 1)
        } else {
            fragments.getOrNull(position - 1)
        }

        val currentColor = getFragmentColor(currentFragment)
        val nextColor = getFragmentColor(nextFragment)

        if (currentColor != null && nextColor != null) {
            val blendedColor = blendColors(currentColor, nextColor, positionOffset)
            updateStatusBarAppearance(blendedColor)
        } else if (currentColor != null) {
            updateStatusBarAppearance(currentColor)
        }
    }

    private fun updateForFragment(fragment: Fragment?) {
        val color = getFragmentColor(fragment)
        color?.let { updateStatusBarAppearance(it) }
    }

    private fun getFragmentColor(fragment: Fragment?): Int? {
        return fragment?.view?.let { view ->
            val hash = view.hashCode()
            colorCache[hash] ?: run {
                val color = getStatusBarColorFromView(view)
                colorCache[hash] = color
                color
            }
        }
    }

    private fun getStatusBarColorFromView(view: View, yOffset: Int = 0): Int {
        if (!view.isAttachedToWindow || view.visibility != View.VISIBLE) {
            return Color.WHITE
        }

        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val y = location[1] + yOffset

        if (y < 0 || y >= view.resources.displayMetrics.heightPixels) {
            return Color.WHITE
        }

        // 对于滚动视图，获取整个可见区域的截图
        if (view is NestedScrollView || view is ViewPager || view is ViewPager2) {
            val visibleRect = Rect()
            view.getGlobalVisibleRect(visibleRect)
            return view.createBitmapFromView(
                visibleRect.width(),
                1, // 高度为1像素，足够获取颜色
                0,
                y.coerceAtLeast(0)
            )?.getPixel(0, 0) ?: Color.WHITE
        }

        return view.createBitmapFromView(1, 1, 0, y.coerceAtLeast(0))?.getPixel(0, 0) ?: Color.WHITE
    }

    private fun updateStatusBarAppearance(color: Int, enableFilter: Boolean = true) {
        if (isUpdating.getAndSet(true)) return

        try {
            val isLight = isColorLight(color)

            // 避免不必要的更新
            if (enableFilter && currentLightStatusBar == isLight) return
            currentLightStatusBar = isLight

            if (isAndroid6Plus) {
                WindowCompat.getInsetsController(
                    activity.window,
                    activity.window.decorView
                ).isAppearanceLightStatusBars = isLight
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.window.decorView.windowInsetsController?.setSystemBarsAppearance(
                    if (isLight) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            }
        } finally {
            isUpdating.set(false)
        }
    }

    private fun isColorLight(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) +
                0.587 * Color.green(color) +
                0.114 * Color.blue(color)) / 255
        return darkness < 0.5
    }

    private fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
        val inverseRatio = 1f - ratio
        val r = (Color.red(color1) * inverseRatio + Color.red(color2) * ratio).toInt()
        val g = (Color.green(color1) * inverseRatio + Color.green(color2) * ratio).toInt()
        val b = (Color.blue(color1) * inverseRatio + Color.blue(color2) * ratio).toInt()
        return Color.rgb(r, g, b)
    }

    private fun View.createBitmapFromView(width: Int, height: Int, x: Int, y: Int): Bitmap? {
        if (width <= 0 || height <= 0) return null

        val bitmap = try {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
                val canvas = Canvas(it)
                canvas.translate(-x.toFloat(), -y.toFloat())
                this.draw(canvas)
            }
        } catch (e: Exception) {
            null
        }
        return bitmap
    }

    private fun shouldThrottle(): Boolean {
        val currentTime = System.currentTimeMillis()
        return if (currentTime - lastUpdateTime < throttleTimeMs) {
            true
        } else {
            lastUpdateTime = currentTime
            false
        }
    }

    companion object {

        private val instances =
            Collections.synchronizedMap(WeakHashMap<Activity, StatusBarManager>())
        private val decorViewListeners =
            mutableMapOf<View, ViewTreeObserver.OnGlobalLayoutListener>()
        private val layoutListeners =
            Collections.synchronizedMap(WeakHashMap<View, ViewTreeObserver.OnGlobalLayoutListener>())
        private val scrollListeners =
            Collections.synchronizedMap(WeakHashMap<View, ViewTreeObserver.OnScrollChangedListener>())
        private val pageChangeCallbacks =
            Collections.synchronizedMap(WeakHashMap<ViewPager2, ViewPager2.OnPageChangeCallback>())
        private val pageChangeListeners =
            Collections.synchronizedMap(WeakHashMap<ViewPager, ViewPager.OnPageChangeListener>())

        // Rect缓存
        private val statusBarRect by lazy { Rect() }
        private val viewVisibleRect by lazy { Rect() }
        private val statusBarHeight by lazy { appCtx.statusBarHeight }

        /**
         * 获取StatusBarManager实例
         */
        @JvmStatic
        fun with(activity: Activity): StatusBarManager {
            return instances.getOrPut(activity) { StatusBarManager(activity) }
        }
    }
}