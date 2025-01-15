package io.core

import android.app.Application
import android.content.res.Resources
import android.util.TypedValue
import io.core.common.helper.LifecycleHelp
import io.core.other.CrashHandler
import kotlin.properties.Delegates

object Android {

    // 全局APPLICATION上下文
    private lateinit var _context: Application

    // 是否是DEBUG模式
    private var _debug: Boolean = true

    // 获取全局APPLICATION上下文
    val context: Application
        get() {
            if (!::_context.isInitialized) {
                throw IllegalStateException("Android context has not been initialized")
            }
            return _context
        }

    // 获取是否为DEBUG模式
    val debug: Boolean
        get() = _debug

    // 设置主Activity
    var homeActivity: Class<*>? = null

    /**
     * 初始化FyCore全局APPLICATION上下文
     */
    fun initialize(application: Application, debug: Boolean = true) {
        if (::_context.isInitialized) return
        _debug = debug
        _context = application
        // 注册全局CrashHandler
        CrashHandler.register(application)
        // 注册Activity生命周期回调
        application.registerActivityLifecycleCallbacks(LifecycleHelp)
    }
}