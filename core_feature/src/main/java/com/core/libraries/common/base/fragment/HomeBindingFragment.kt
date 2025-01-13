package com.core.libraries.common.base.fragment

import android.os.Bundle
import android.view.View
import androidx.viewbinding.ViewBinding
import com.core.libraries.R
import com.core.libraries.common.base.activity.BaseActivity
import com.core.libraries.common.util.ext.ui.BarColor
import com.core.libraries.common.util.ext.isNull
import com.gyf.immersionbar.ImmersionBar

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/7 17:20
 * @description
 * @author Yuan
 */
abstract class HomeBindingFragment<VB : ViewBinding, A : BaseActivity> :
    ReflectBindingFragment<VB, A>() {

    /** 状态栏沉浸 */
    private var immersionBar: ImmersionBar? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        getStatusBarConfig().init()

    }

    override fun onResume() {
        super.onResume()
        // 重新初始化状态栏
        getStatusBarConfig().init()
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
}