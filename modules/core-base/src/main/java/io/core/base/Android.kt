package io.core.base

import android.app.Application
import androidx.core.content.FileProvider

/**
 * 全局Application上下文扩展属性
 */
inline val appCtx get() = Android.context

/**
 * FyCore 框架核心入口类
 * 负责全局上下文管理和模块初始化调度
 */
object Android {

    private var _context: Application? = null

    private var _debug: Boolean = true

    private val initializers = mutableListOf<Initializer>()

    @JvmStatic
    val context: Application
        get() = _context ?: throw IllegalStateException("请先调用 initialize() 方法完成初始化")

    @JvmStatic
    var debug: Boolean
        @JvmName("isDebug")
        get() = _debug
        @JvmName("updateMode")
        set(value) {
            _debug = value
        }

    /**
     * 注册模块初始化器
     */
    @JvmStatic
    fun registerInitializer(initializer: Initializer) {
        if (!initializers.contains(initializer)) {
            initializers.add(initializer)
        }
    }

    /**
     * 批量注册模块初始化器
     */
    @JvmStatic
    fun registerInitializers(vararg initializerList: Initializer) {
        initializerList.forEach { initializer ->
            registerInitializer(initializer)
        }
    }

    /**
     * 初始化FyCore全局APPLICATION上下文
     */
    @JvmStatic
    @JvmOverloads
    fun initialize(
        application: Application,
        debug: Boolean = true
    ) {
        _context?.run { return }
        _debug = debug
        _context = application

        initializers.forEach { initializer ->
            initializer.onInit(application, debug)
        }
    }

    /**
     * 清除所有初始化器
     */
    @JvmStatic
    fun clearInitializers() {
        initializers.clear()
    }

    /**
     * 清除数据
     */
    @JvmStatic
    fun clearData() = runCatching {
        initializers.filterIsInstance<DataClearable>().forEach { it.onClearData() }
    }
}

/**
 * 模块初始化器接口
 * 各模块实现此接口以完成模块初始化
 */
interface Initializer {
    /**
     * 模块初始化回调
     * @param application Application实例
     * @param debug 是否为调试模式
     */
    fun onInit(application: Application, debug: Boolean)
}

/**
 * 数据清理接口
 * 需要支持数据清理的模块实现此接口
 */
interface DataClearable {
    /**
     * 清理模块数据
     */
    fun onClearData()
}

/**
 * FileProvider实现类
 */
class XFileProvider : FileProvider()
