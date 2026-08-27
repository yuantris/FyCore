package io.core

import android.app.Activity
import android.app.Application
import androidx.core.content.FileProvider
import io.core.common.CoreConfig
import io.core.common.util.log.LogCat
import io.core.common.util.log.bury.AppLog

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
        get() = _context ?: applicationByReflect()?.also {
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
     * 功能模块初始化/清理钩子。
     * kernel 保持零依赖：track/crash/nav/event/storage 等模块的启动逻辑
     * 由聚合层（fy-core）通过 [registerInitHook] 注册，业务方调用方式不变。
     */
    internal val initHooks = mutableListOf<(Application, Boolean) -> Unit>()
    internal val clearHooks = mutableListOf<() -> Unit>()

    // 顶层Activity提供者 / 结束全部Activity处理器：由具备栈管理能力的模块(track)注册
    internal var topActivityProvider: (() -> Activity?)? = null
    internal var finishAllActivitiesHandler: (() -> Unit)? = null

    fun registerInitHook(hook: (Application, Boolean) -> Unit) {
        initHooks += hook
    }

    fun registerClearHook(hook: () -> Unit) {
        clearHooks += hook
    }

    fun registerTopActivityProvider(provider: () -> Activity?) {
        topActivityProvider = provider
    }

    fun registerFinishAllActivitiesHandler(handler: () -> Unit) {
        finishAllActivitiesHandler = handler
    }

    /** 当前栈顶Activity，未注册提供者时返回 null */
    @JvmStatic
    val topActivity: Activity?
        get() = try { topActivityProvider?.invoke() } catch (_: Throwable) { null }

    /** 结束全部Activity，未注册处理器时为空操作 */
    @JvmStatic
    fun finishAllActivities() {
        try { finishAllActivitiesHandler?.invoke() } catch (_: Throwable) {}
    }

    private fun applicationByReflect(): Application? = runCatching {
        Class.forName("android.app.ActivityThread")
            .getDeclaredMethod("currentApplication")
            .invoke(null) as? Application
    }.getOrNull()

    /**
     * 初始化FyCore全局APPLICATION上下文
     */
    @JvmStatic
    @JvmOverloads
    fun initialize(
        application: Application,
        debug: Boolean = true,
        key: String? = null
    ) {
        _context?.run { return LogCat.e("<-------FyCore已经初始化过了，请勿重复初始化------->") }

        // 初始化配置
        _debug = debug
        _context = application

        key?.run { CoreConfig.setToken(this) }
        AppLog.initialize()
        initHooks.forEach { hook ->
            runCatching { hook(application, debug) }
                .onFailure { LogCat.e("FyCore init hook failed: ${it.message}") }
        }
    }

    @JvmStatic
    fun clearData() = runCatching {
        clearHooks.forEach { hook -> runCatching { hook() } }
    }
}

class XFileProvider : FileProvider() {
    override fun onCreate(): Boolean {
        return super.onCreate()
    }
}
