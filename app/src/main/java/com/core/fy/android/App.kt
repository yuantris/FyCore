package com.core.fy.android

import android.app.Application
import com.core.libraries.Android

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2024/12/24 8:54
 * @description
 * @author Yuan
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Android.initialize(this)
        // SoftKeyboardGlobal.install(this, false)
    }
}