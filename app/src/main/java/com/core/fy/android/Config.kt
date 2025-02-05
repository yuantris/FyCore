package com.core.fy.android

import com.core.fy.android.constants.PreferKey
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
}