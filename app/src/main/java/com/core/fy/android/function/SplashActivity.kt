package com.core.fy.android.function

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.Bundle
import com.core.fy.android.MainActivity
import com.core.fy.android.databinding.ActivitySplashBinding
import com.core.libraries.base.activity.ReflectBindingActivity
import com.core.libraries.base.ext.startActivity
import com.core.libraries.base.ext.startActivityNoAnim
import com.gyf.immersionbar.BarHide
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
 * 2024/12/24 18:05
 * @description
 * @author Yuan
 */
class SplashActivity : ReflectBindingActivity<ActivitySplashBinding>() {
    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)
        // 问题及方案：https://www.cnblogs.com/net168/p/5722752.html
        // 如果当前 Activity 不是任务栈中的第一个 Activity
        if (!isTaskRoot) {
            val intent: Intent? = intent
            // 如果当前 Activity 是通过桌面图标启动进入的
            if (((intent != null) && intent.hasCategory(Intent.CATEGORY_LAUNCHER)
                        && (Intent.ACTION_MAIN == intent.action))
            ) {
                // 对当前 Activity 执行销毁操作，避免重复实例化入口
                finish()
                return
            }
        }
    }

    override fun setListener() {
        super.setListener()
        // 设置动画监听
        mBinding.lavSplashLottie.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mBinding.lavSplashLottie.removeAnimatorListener(this)
                startActivityNoAnim(MainActivity::class.java)
                finish()
            }
        })
    }

    override fun createStatusBarConfig(): ImmersionBar {
        return super.createStatusBarConfig()
            // 隐藏状态栏和导航栏
            .hideBar(BarHide.FLAG_HIDE_BAR)
    }

    override fun onBackPressed() {
        // 禁用返回键
        //super.onBackPressed();
    }

    override fun onDestroy() {
        // 因为修复了一个启动页被重复启动的问题，所以有可能 Activity 还没有初始化完成就已经销毁了
        // 所以如果需要在此处释放对象资源需要先对这个对象进行判空，否则可能会导致空指针异常
        super.onDestroy()
    }
}