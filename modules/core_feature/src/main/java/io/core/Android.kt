package io.core

import android.app.Application
import androidx.core.content.FileProvider
import io.core.common.helper.AppLifecycleTracker
import io.core.common.util.log.LogCat
import io.core.common.util.tools.Preferences
import io.core.engine.livebus.LiveEventBus
import io.core.engine.livebus.logger.DefaultLogger
import io.core.other.CrashHandler

inline val appCtx get() = Android.context

object Android {

    // 全局APPLICATION上下文
    private lateinit var _context: Application

    // 是否是DEBUG模式
    private var _debug: Boolean = true

    @JvmStatic
    val context: Application // 获取全局APPLICATION上下文
        get() {
            if (!::_context.isInitialized) {
                AppLifecycleTracker.getApplicationReflect()?.let {
                    _context = it
                } ?: throw IllegalStateException("请先调用 initialize() 方法完成初始化")
            }
            return _context
        }

    @JvmStatic
    val debug: Boolean // 获取是否为DEBUG模式
        @JvmName("isDebug")
        get() = _debug


    /**
     * 初始化FyCore全局APPLICATION上下文
     */
    @JvmStatic
    fun initialize(application: Application, debug: Boolean = true) {
        if (::_context.isInitialized) {
            LogCat.e("<-------FyCore已经初始化过了，请勿重复初始化------->")
            return
        }
        _debug = debug
        _context = application
        // 注册Activity生命周期回调
        AppLifecycleTracker.init(application)
        // 注册全局CrashHandler
        CrashHandler.register(application)
        // 初始化日志
        LogCat.setDebug(debug)
        // LiveEventBus 初始化
        LiveEventBus.config()
            .lifecycleObserverAlwaysActive(true)
            .autoClear(true)
            .enableLogger(debug)
            .setLogger(DefaultLogger())
    }

    @JvmStatic
    fun clearData() {
        // 清除sp数据
        Preferences.clear()
    }

}

class CoreFileProvider : FileProvider() {
    override fun onCreate(): Boolean {
        return super.onCreate()
    }
}