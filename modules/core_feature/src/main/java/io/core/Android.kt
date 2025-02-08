package io.core

import android.app.Application
import io.core.common.helper.LifecycleHelp
import io.core.common.util.log.LogCat
import io.core.common.util.tools.Preferences
import io.core.engine.livebus.LiveEventBus
import io.core.engine.livebus.logger.DefaultLogger
import io.core.other.CrashHandler

object Android {

    // 全局APPLICATION上下文
    private lateinit var _context: Application

    // 是否是DEBUG模式
    private var _debug: Boolean = true

    // 获取全局APPLICATION上下文
    val context: Application
        get() {
            if (!::_context.isInitialized) {
                throw IllegalStateException("请先调用 initialize() 方法完成初始化")
            }
            return _context
        }

    // 获取是否为DEBUG模式
    val debug: Boolean
        get() = _debug

    // 设置主Activity(启动页要跳转的Activity)
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
        // 初始化日志
        LogCat.setDebug(debug)
        // 注册Activity生命周期回调
        application.registerActivityLifecycleCallbacks(LifecycleHelp)
        // LiveEventBus 初始化
        LiveEventBus.config()
            .lifecycleObserverAlwaysActive(true)
            .autoClear(true)
            .enableLogger(debug)
            .setLogger(DefaultLogger())
    }

    fun clearData() {
        // 清除sp数据
        Preferences.clear()
    }

}