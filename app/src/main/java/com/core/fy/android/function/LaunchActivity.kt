package com.core.fy.android.function

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.core.fy.android.MainActivity
import com.core.fy.android.databinding.ActivityLaunchBinding
import io.core.ui.base.component.activity.ReflectBindingActivity
import com.gyf.immersionbar.BarHide
import com.gyf.immersionbar.ImmersionBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
# ██████████
# █▄█████▄�?
# █▼▼▼▼▼
# �?
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载…�?
 * 2024/12/24 15:21
 * @description
 * @author Yuan
 */
class LaunchActivity : ReflectBindingActivity<ActivityLaunchBinding>() {
    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)
        lifecycleScope.launch {
            delay(1000)
            startActivity(Intent(this@LaunchActivity, MainActivity::class.java))
            finish()
        }

    }

    override fun createStatusBarConfig(): ImmersionBar {
        return super.createStatusBarConfig()
            // 隐藏状态栏和导航栏
            .hideBar(BarHide.FLAG_HIDE_BAR)
    }
}