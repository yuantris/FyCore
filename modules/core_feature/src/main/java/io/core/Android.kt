package io.core

import android.app.Application
import androidx.core.content.FileProvider
import io.core.common.helper.AppLifecycleTracker
import io.core.common.util.extensions.currentTime
import io.core.common.util.extensions.logD
import io.core.common.util.log.LogCat
import io.core.common.util.log.bury.AppLog
import io.core.common.util.tools.Preferences
import io.core.engine.livebus.LiveEventBus
import io.core.engine.livebus.logger.DefaultLogger
import io.core.engine.storage.StorageFactory
import io.core.engine.storage.storage
import io.core.other.CrashHandler
import io.core.other.IntentData

inline val appCtx get() = Android.context

object Android {

    // 全局APPLICATION上下文
    private var _context: Application? = null

    // 是否是DEBUG模式
    private var _debug: Boolean = true
        set(value) {
            field = value
            LogCat.setDebug(value)
            AppLog.updateMode(value)
        }

    @JvmStatic
    val context: Application
        get() = _context ?: AppLifecycleTracker.getApplicationReflect()?.also {
            _context = it
        } ?: throw IllegalStateException("请先调用 initialize() 方法完成初始化")

    @JvmStatic
    var debug: Boolean
        @JvmName("isDebug")
        get() = _debug
        @JvmName("updateMode")
        set(value) {
            _debug = value
        }

    /**
     * 初始化FyCore全局APPLICATION上下文
     */
    @JvmStatic
    fun initialize(application: Application, debug: Boolean = true) {
        _context?.run { return LogCat.e("<-------FyCore已经初始化过了，请勿重复初始化------->") }

        // 初始化配置
        _debug = debug
        _context = application

        currentTime.logD()
        application.run {
            // 注册Activity生命周期回调
            AppLifecycleTracker.init(this)
            // 注册全局CrashHandler
            CrashHandler.register(this)
            // 初始化日志
            AppLog.initialize()
            // LiveEventBus 初始化
            LiveEventBus.config().apply {
                lifecycleObserverAlwaysActive(true)
                autoClear(true)
                enableLogger(debug)
                setLogger(DefaultLogger())
            }
        }
    }

    @JvmStatic
    fun clearData() = runCatching {
        IntentData.clear()
        // 清除sp数据
        Preferences.clear()
        storage.takeIf { StorageFactory.isInit() }?.clear()
    }


}

class XFileProvider : FileProvider() {
    override fun onCreate(): Boolean {
        return super.onCreate()
    }
}