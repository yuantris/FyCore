package io.core.common

import io.core.R
import io.core.common.util.ext.appCtx
import io.core.common.util.ext.ui.getCompatColor

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
object CoreConfigs {
    /*AndroidAlertBuilder的按钮色值*/
    var DIALOG_BUTTON_POSITIVE_COLOR = appCtx.getCompatColor(R.color.common_accent_color)
    var DIALOG_BUTTON_NEGATIVE_COLOR = appCtx.getCompatColor(R.color.common_accent_color)
}