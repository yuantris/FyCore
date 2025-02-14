package com.core.fy.android.function

import android.view.Gravity
import com.core.fy.android.MainActivity
import com.core.fy.android.R
import com.core.fy.android.constants.PreferKey
import io.core.common.base.component.activity.BaseGuideActivity
import io.core.common.base.component.activity.GuideConfig
import io.core.common.util.ext.cool.dpToPx
import io.core.common.util.ext.ui.startNoTransition
import io.core.common.util.tools.Preferences
import io.core.other.IntentData

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/2/10 8:38
 * @description
 * @author Yuan
 */
class GuideActivity : BaseGuideActivity() {

    override fun getGuideConfig() = GuideConfig(
        guideImages = listOf(R.drawable.splash_1, R.drawable.splash_2, R.drawable.splash_3),
        enterButtonRes = 0,
        enableEnterButton = true,
        enterButtonMargin = 80,
        showIndicator = false,
        indicatorMargin = 100,
        indicatorDotSelected = R.drawable.ic_daytime,
        indicatorDotNormal = R.drawable.ic_brightness,
    )

    override fun onEnterClicked() {
        startNoTransition(MainActivity::class.java, finish = true)
    }
}