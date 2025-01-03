package com.core.libraries.base.activity

import android.os.Bundle
import android.view.View
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import com.core.libraries.R
import com.core.libraries.base.action.TitleBarAction
import com.core.libraries.base.ext.BarColor
import com.core.libraries.base.ext.isNotNull
import com.core.libraries.base.ext.isNull
import com.core.libraries.base.ext.logD
import com.core.libraries.view.TitleBar
import com.gyf.immersionbar.ImmersionBar

abstract class BaseActivity : AppCompatActivity(), TitleBarAction {
    private val TAG by lazy { "BaseActivity_" }

    /** 标题栏对象 */
    private var titleBar: TitleBar? = null

    /** 状态栏沉浸 */
    private var immersionBar: ImmersionBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentViewBind())
        initial(savedInstanceState)
        setListener()
        observers()
    }


    protected open fun initial(savedInstanceState: Bundle?) {
        val titleBar = getTitleBar()
        getStatusBarConfig().init()
        // 设置标题栏沉浸
        titleBar.isNotNull {
            ImmersionBar.setTitleBar(this, titleBar)
        }
    }

    protected open fun setListener() {}
    protected open fun observers() {}

    open fun contentViewBind(): View? {
        return null
    }

    /**
     * 获取状态栏沉浸的配置对象
     */
    open fun getStatusBarConfig(): ImmersionBar {
        immersionBar.isNull {
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
        titleBar.isNull { titleBar = obtainTitleBar(findViewById(Window.ID_ANDROID_CONTENT)) }
        "titleBar= ${titleBar == null}".logD(TAG)
        return titleBar
    }

}