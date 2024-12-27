package com.core.libraries.base.ext

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2024/12/27 11:16
 * @description
 * @author Yuan
 */

/**
 * @return 上下文中的Activity对象
 */
fun Context.getActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
}