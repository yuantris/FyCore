package io.core.nav

import android.app.Application
import io.core.base.Android
import io.core.base.DataClearable
import io.core.base.Initializer
import io.core.engine.EngineInitializer
import io.core.nav.other.AppLauncher
import io.core.nav.other.CrashHandler
import io.core.nav.other.IntentData
import io.core.ui.UiInitializer
import io.core.utils.UtilsInitializer

/**
 * FyCore 框架入口
 * 提供一键初始化所有模块的便捷方法
 * 
 * 使用示例：
 * ```kotlin
 * class MyApplication : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         FyCore.init(this)
 *     }
 * }
 * ```
 */
object FyCore {

    /**
     * 初始化FyCore框架（自动注册所有模块）
     * 
     * @param application Application实例
     * @param debug 是否为调试模式，默认true
     * @param enableUtils 是否启用utils模块，默认true
     * @param enableUi 是否启用ui模块，默认true
     * @param enableEngine 是否启用engine模块，默认true
     * @param enableNav 是否启用nav模块，默认true
     */
    @JvmStatic
    @JvmOverloads
    fun init(
        application: Application,
        debug: Boolean = true,
        enableUtils: Boolean = true,
        enableUi: Boolean = true,
        enableEngine: Boolean = true,
        enableNav: Boolean = true
    ) {
        val initializers = mutableListOf<Initializer>()
        if (enableUtils) initializers.add(UtilsInitializer)
        if (enableUi) initializers.add(UiInitializer)
        if (enableEngine) initializers.add(EngineInitializer)
        if (enableNav) initializers.add(NavInitializer)
        
        Android.registerInitializers(*initializers.toTypedArray())
        Android.initialize(application, debug)
    }

    /**
     * 仅注册初始化器，不执行初始化
     * 用于需要自定义初始化时机的场景
     */
    @JvmStatic
    @JvmOverloads
    fun registerAllInitializers(
        enableUtils: Boolean = true,
        enableUi: Boolean = true,
        enableEngine: Boolean = true,
        enableNav: Boolean = true
    ) {
        val initializers = mutableListOf<Initializer>()
        if (enableUtils) initializers.add(UtilsInitializer)
        if (enableUi) initializers.add(UiInitializer)
        if (enableEngine) initializers.add(EngineInitializer)
        if (enableNav) initializers.add(NavInitializer)
        
        Android.registerInitializers(*initializers.toTypedArray())
    }

    /**
     * 获取全局上下文
     */
    @JvmStatic
    val context: Application
        get() = Android.context

    /**
     * 是否为调试模式
     */
    @JvmStatic
    val isDebug: Boolean
        get() = Android.debug

    /**
     * 清除所有数据
     */
    @JvmStatic
    fun clearData() {
        Android.clearData()
    }
}

/**
 * core-nav 模块初始化器
 * 负责初始化导航和崩溃处理组件
 */
object NavInitializer : Initializer, DataClearable {
    
    override fun onInit(application: Application, debug: Boolean) {
        NavigationManager.initialize()
        CrashHandler.register(application)
    }

    override fun onClearData() {
        IntentData.clear()
        AppLauncher.clearCache()
    }
}
