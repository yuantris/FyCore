package com.core.fy.android

import android.app.Application
import com.core.libraries.other.keyboard.SoftKeyboardGlobal

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

    companion object {
        lateinit var context: Application
    }

    override fun onCreate() {
        context = this
        super.onCreate()
        // SoftKeyboardGlobal.install(this, false)
    }
}