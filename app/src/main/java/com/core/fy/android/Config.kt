package com.core.fy.android

import com.core.fy.android.constants.PreferKey
import io.core.appCtx
import io.core.common.CoreConfig
import io.core.engine.storage.getWithAnnotation
import io.core.engine.storage.storage

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
    var isDisplaySplashAnim = storage.getWithAnnotation<Boolean>(PreferKey.SPLASH_ANIM)
    var isDisplayGuide = storage.getWithAnnotation<Boolean>(PreferKey.GUIDE_PAGE)
    var isDisplayHomeSkeletonAnim = storage.getWithAnnotation<Boolean>(PreferKey.HOME_SKELETON_ANIM)

    init {
        CoreConfig.CRASH_MULTI_PROCESS = true
        CoreConfig.CRASH_AFTER_JUMP = MainActivity::class.java
        CoreConfig.alert_positive_color = appCtx.getColor(R.color.md_indigo_500)
        CoreConfig.alert_negative_color = appCtx.getColor(R.color.md_red_300)
    }

}