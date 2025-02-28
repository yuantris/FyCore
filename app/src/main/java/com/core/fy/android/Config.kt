package com.core.fy.android

import com.core.fy.android.constants.PreferKey
import io.core.appCtx
import io.core.common.CoreConfig
import io.core.common.util.tools.Preferences

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/2/5 16:42
 * @description
 * @author Yuan
 */
object Config {
    // 是否显示启动动画
    var isDisplaySplashAnim = Preferences.getValue(PreferKey.isDisplaySplashAnim, true)
    var isDisplayGuide = Preferences.getValue(PreferKey.isDisplayGuide, true)
    var isDisplayHomeSkeletonAnim = Preferences.getValue(PreferKey.isDisplayHomeSkeletonAnim, true)

    init {
        CoreConfig.CRASH_AFTER_JUMP = MainActivity::class.java
        CoreConfig.alert_positive_color = appCtx.getColor(R.color.md_indigo_500)
        CoreConfig.alert_negative_color = appCtx.getColor(R.color.md_red_300)
    }

}