package io.core.common.base.component.activity

import android.os.Bundle
import android.view.View
import android.view.Window
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import com.gyf.immersionbar.ImmersionBar
import io.core.R
import io.core.common.base.action.BundleAction
import io.core.common.base.action.TitleBarAction
import io.core.common.util.ext.addCallback
import io.core.common.util.ext.ifNotNull
import io.core.common.util.ext.ifNull
import io.core.common.util.ext.ui.BarColor
import io.core.common.util.log.logD
import io.core.widget.layout.TitleBar

abstract class BaseActivity : AppCompatActivity(), TitleBarAction, BundleAction {
    private val TAG by lazy { "BaseActivity_" }

    /** 标题栏对象 */
    private var titleBar: TitleBar? = null

    /** 状态栏沉浸 */
    private var immersionBar: ImmersionBar? = null

    /** 是否接管返回键 */
    private var isTakeOverBackPressed = false

    override fun onCreate(savedInstanceState: Bundle?) {
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
        titleBar.ifNotNull {
            ImmersionBar.setTitleBar(this, it)
        }
        // 设置返回键拦截
        onBackPressedDispatcher.addCallback(this, enabled = isTakeOverBackPressed) {
            onBackPressedCall()
        }
    }

    protected open fun setListener() {}
    protected open fun observers() {}

    /**
     * 设置是否接管返回键，默认不接管，如果要接管，请重写onBackPressedCall方法，
     * 并在initial方法super之前setTakeOverBackPressed(true)
     */
    protected open fun onBackPressedCall() {}

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
        immersionBar.ifNull {
            immersionBar = createStatusBarConfig()
        }
        return immersionBar!!
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

    override fun getTitleBar(): TitleBar? {
        titleBar.ifNull { titleBar = obtainTitleBar(findViewById(Window.ID_ANDROID_CONTENT)) }
        "titleBar= ${titleBar == null}".logD(TAG)
        return titleBar
    }

    override fun getBundle(): Bundle? {
        return intent.extras
    }

}