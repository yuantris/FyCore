package io.core.common

import io.core.R
import io.core.appCtx
import io.core.common.util.extensions.ui.getCompatColor

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/20 18:01
 * @description
 * @author Yuan
 */
object CoreConfig {
    private val accentColor = appCtx.getCompatColor(R.color.common_accent_color)

    /*AndroidAlertBuilder的按钮色值*/
    var alert_positive_color = accentColor
    var alert_negative_color = accentColor

    @JvmStatic
    var crashAfterJumpActivity: Class<*>? = null // 设置闪退后要跳转的Activity
}