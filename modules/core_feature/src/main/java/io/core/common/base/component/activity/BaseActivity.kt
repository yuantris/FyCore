package io.core.common.base.component.activity

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.Window
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import com.gyf.immersionbar.ImmersionBar
import io.core.R
import io.core.common.base.action.BundleAction
import io.core.common.base.action.TitleBarAction
import io.core.common.util.DiveGestureLine
import io.core.common.util.extensions.addCallback
import io.core.common.util.extensions.ui.BarColor
import io.core.common.util.extensions.ui.ScreenOrientation
import io.core.common.util.extensions.ui.adaptStatusBarToView
import io.core.constant.DeviceOS
import io.core.widget.layout.TitleBar

abstract class BaseActivity : AppCompatActivity(), TitleBarAction, BundleAction {
    private val TAG by lazy { "BaseActivity_" }

    /** 标题栏对象 */
    private var titleBar: TitleBar? = null

    /** 状态栏沉浸 */
    private var immersionBar: ImmersionBar? = null

    /** 是否接管返回键 */
    private var isTakeOverBackPressed = false

    /** 是否启用横竖屏方向锁定*/
    var isLockOrientation = true

    /** 当前Activity的简单类名(只读) */
    val simpleName: String by lazy { this::class.java.simpleName }

    override fun onCreate(savedInstanceState: Bundle?) {
        configOrientation()
        super.onCreate(savedInstanceState)
        setContentView(contentViewBind())
        initial(savedInstanceState)
        setListener()
        observers()
    }

    @CallSuper
    protected open fun initial(savedInstanceState: Bundle?) {
        val titleBar = getTitleBar()
        getStatusBarConfig().init()
        // 设置标题栏沉浸
        titleBar?.let {
            ImmersionBar.setTitleBar(this, it)
        }
        adaptOS()
        // 设置返回键拦截
        onBackPressedDispatcher.addCallback(this, enabled = isTakeOverBackPressed) {
            onBackPressedCall()
        }
    }

    final override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            supportFinishAfterTransition()
            return true
        }
        return onCompatOptionsItemSelected(item)
    }

    open fun onCompatOptionsItemSelected(item: MenuItem) = super.onOptionsItemSelected(item)


    protected open fun setListener() = Unit
    protected open fun observers() = Unit

    /**
     * 设置是否接管返回键，默认不接管，如果要接管，请重写onBackPressedCall方法，
     * 并在initial方法super之前setTakeOverBackPressed(true)
     */
    protected open fun onBackPressedCall() = Unit

    /**
     * 设置是否接管返回键，默认不接管
     * TODO 在initial方法super之前调用生效
     */
    open fun setTakeOverBackPressed(takeOverBackPressed: Boolean) {
        isTakeOverBackPressed = takeOverBackPressed
    }

    open fun contentViewBind(): View? {
        return null
    }

    /**
     * 获取状态栏沉浸的配置对象
     */
    open fun getStatusBarConfig(): ImmersionBar {
        return immersionBar ?: createStatusBarConfig().also { immersionBar = it }
    }

    /**
     * 初始化沉浸式状态栏
     */
    protected open fun createStatusBarConfig(): ImmersionBar {
        return ImmersionBar.with(this) // 默认状态栏字体颜色为黑色
            .statusBarDarkFont(getStatusBarColor() == BarColor.BLACK) // 指定导航栏背景颜色
            .navigationBarColor(R.color.color_white) // 状态栏字体和导航栏内容自动变色，必须指定状态栏颜色和导航栏颜色才可以自动变色
            .autoDarkModeEnable(true, 0.2f)
    }


    protected open fun getStatusBarColor(): BarColor {
        return BarColor.BLACK // 根据实际情况返回 BarColor.BLACK 或 BarColor.WHITE
    }

    protected open fun getNavigationBarColor(): BarColor {
        return BarColor.BLACK // 根据实际情况返回 BarColor.BLACK 或 BarColor.WHITE
    }

    protected open fun getLockOrientation(): ScreenOrientation {
        return ScreenOrientation.VERTICAL // 根据实际情况返回
    }

    // 定义针对不同设备的适配逻辑，默认实现
    protected open var xiaomiAdapt: (Window) -> Unit = { window ->
        DiveGestureLine.adaptXiaomi(window)
    }
    protected open var huaweiAdapt: (Window) -> Unit = { window ->
        DiveGestureLine.adaptHuawei(window)
    }
    protected open var oppoAdapt: (Window) -> Unit = { window ->
        DiveGestureLine.adaptOPPO(window)
    }
    protected open var vivoAdapt: (Window) -> Unit = { window ->
    }

    protected open fun adaptOS() {
        when (DeviceOS.rom) {
            DeviceOS.Rom.HyperOS -> xiaomiAdapt(window)
            DeviceOS.Rom.HarmonyOS -> huaweiAdapt(window)
            DeviceOS.Rom.ColorOS -> oppoAdapt(window)
            DeviceOS.Rom.OriginOS -> vivoAdapt(window)

            else -> Unit
        }
        adaptStatusBarToView(window.decorView)
    }

    override fun getTitleBar(): TitleBar? {
        titleBar = titleBar ?: obtainTitleBar(findViewById(Window.ID_ANDROID_CONTENT))
        return titleBar
    }

    override fun getBundle(): Bundle? {
        return intent.extras
    }

    private fun configOrientation() {
        if (isLockOrientation) {
            requestedOrientation = if (getLockOrientation() == ScreenOrientation.VERTICAL) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }
}