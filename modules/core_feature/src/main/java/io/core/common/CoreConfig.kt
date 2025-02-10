package io.core.common

import io.core.R
import io.core.common.util.ext.appCtx
import io.core.common.util.ext.ui.getCompatColor
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
 * 2025/1/20 18:01
 * @description
 * @author Yuan
 */
object CoreConfig {
    private val accentColor = appCtx.getCompatColor(R.color.common_accent_color)

    /*AndroidAlertBuilder的按钮色值*/
    var DIALOG_BUTTON_POSITIVE_COLOR = accentColor
    var DIALOG_BUTTON_NEGATIVE_COLOR = accentColor
}