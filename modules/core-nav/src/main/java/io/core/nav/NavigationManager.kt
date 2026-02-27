package io.core.nav

import android.app.Activity
import android.content.Context
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * 全局导航管理�?- 单例模式
 * 解决模块化项目中子模块无法访问主app Application的问�?
 */
object NavigationManager {

    private var _navigator: Navigator? = null

    /**
     * 获取导航器实�?
     * @throws IllegalStateException 如果未初始化
     */
    fun getNavigator(): Navigator {
        return _navigator ?: throw IllegalStateException(
            "NavigationManager not initialized. Call initialize() in Application.onCreate()"
        )
    }

    /**
     * 检查是否已初始�?
     */
    fun isInitialized(): Boolean = _navigator != null

    /**
     * 初始化导航器
     * @param configBlock 配置块，用于自定义导航器
     */
    fun initialize(configBlock: Navigator.Builder.() -> Unit) {
        if (_navigator != null) {
            throw IllegalStateException("NavigationManager already initialized")
        }

        // 创建专用的协程作用域
        val navigationScope = CoroutineScope(SupervisorJob())

        val builder = Navigator.builder()
            .setCoroutineScope(navigationScope)

        // 应用配置
        configBlock.invoke(builder)

        _navigator = builder.build()
    }

    /**
     * 初始化导航器（无配置�?
     * 使用默认配置初始化，后续可通过动态配置方法进行设�?
     */
    fun initialize() {
        initialize {
            // 使用默认配置
        }
    }


    /**
     * 清理资源（主要用于测试）
     */
    internal fun reset() {
        _navigator = null
    }

    // ==================== 动态配置方�?====================

    /**
     * 动态添加拦截器（运行时�?
     * @param interceptor 要添加的拦截�?
     * @param priority 优先级，默认�?0。数值越大优先级越高
     * @return 当前 NavigationManager 实例，支持链式调�?
     */
    fun addInterceptor(interceptor: NavigationInterceptor, priority: Int = 0): NavigationManager {
        getNavigator().addInterceptor(interceptor, priority)
        return this
    }

    /**
     * 动态移除拦截器（运行时�?
     * @param interceptor 要移除的拦截器实�?
     * @return 当前 NavigationManager 实例，支持链式调�?
     */
    fun removeInterceptor(interceptor: NavigationInterceptor): NavigationManager {
        getNavigator().removeInterceptor(interceptor)
        return this
    }

    /**
     * 动态移除指定类型的拦截器（运行时）
     * @param interceptorClass 要移除的拦截器类�?
     * @return 当前 NavigationManager 实例，支持链式调�?
     */
    fun removeInterceptor(interceptorClass: Class<out NavigationInterceptor>): NavigationManager {
        getNavigator().removeInterceptor(interceptorClass)
        return this
    }

    /**
     * 动态设置执行器（运行时�?
     * @param executor 新的执行�?
     * @return 当前 NavigationManager 实例，支持链式调�?
     */
    fun setExecutor(executor: NavigationExecutor): NavigationManager {
        getNavigator().setExecutor(executor)
        return this
    }

    /**
     * 动态设置监听器（运行时�?
     * @param listener 新的监听器，null 表示移除监听�?
     * @return 当前 NavigationManager 实例，支持链式调�?
     */
    fun setListener(listener: NavigationListener?): NavigationManager {
        getNavigator().setListener(listener)
        return this
    }

    /**
     * 动态添加路由（运行时）
     * @param route 路由路径
     * @param targetClass 目标 Activity �?
     * @return 当前 NavigationManager 实例，支持链式调�?
     */
    fun addRoute(route: String, targetClass: Class<out Activity>): NavigationManager {
        getNavigator().addRoute(route, targetClass)
        return this
    }

    /**
     * 动态移除路由（运行时）
     * @param route 要移除的路由路径
     * @return 当前 NavigationManager 实例，支持链式调�?
     */
    fun removeRoute(route: String): NavigationManager {
        getNavigator().removeRoute(route)
        return this
    }

    /**
     * 批量动态配置（运行时）
     * @param block 配置块，可以进行多项配置
     * @return 当前 NavigationManager 实例，支持链式调�?
     */
    fun configure(block: DynamicConfigBuilder.() -> Unit): NavigationManager {
        val builder = DynamicConfigBuilder(this)
        builder.block()
        return this
    }

    /**
     * 获取当前配置的拦截器列表（只读）
     */
    fun getInterceptors(): List<NavigationInterceptor> = getNavigator().getInterceptors()

    /**
     * 获取当前配置的执行器
     */
    fun getExecutor(): NavigationExecutor = getNavigator().getExecutor()

    /**
     * 获取当前配置的监听器
     */
    fun getListener(): NavigationListener? = getNavigator().getListener()

    /**
     * 动态配置构建器
     */
    class DynamicConfigBuilder internal constructor(private val manager: NavigationManager) {

        /**
         * 添加拦截�?
         */
        fun addInterceptor(interceptor: NavigationInterceptor, priority: Int = 0) {
            manager.addInterceptor(interceptor, priority)
        }

        /**
         * 移除拦截�?
         */
        fun removeInterceptor(interceptor: NavigationInterceptor) {
            manager.removeInterceptor(interceptor)
        }

        /**
         * 移除指定类型的拦截器
         */
        fun removeInterceptor(interceptorClass: Class<out NavigationInterceptor>) {
            manager.removeInterceptor(interceptorClass)
        }

        /**
         * 设置执行�?
         */
        fun setExecutor(executor: NavigationExecutor) {
            manager.setExecutor(executor)
        }

        /**
         * 设置监听�?
         */
        fun setListener(listener: NavigationListener?) {
            manager.setListener(listener)
        }

        /**
         * 添加路由
         */
        fun addRoute(route: String, targetClass: Class<out Activity>) {
            manager.addRoute(route, targetClass)
        }

        /**
         * 移除路由
         */
        fun removeRoute(route: String) {
            manager.removeRoute(route)
        }
    }
}

/**
 * Context扩展函数，提供更便捷的访问方�?
 */
val navigator: Navigator
    get() = NavigationManager.getNavigator()

inline fun <reified T : Activity> Context.routerTo(builder: NavigationBuilder<T>.() -> Unit = {}): NavigationResult {
    return navigator.to<T>(this)
        .apply(builder)
        .go()
}
inline fun <reified T : Activity> Fragment.routerTo(builder: NavigationBuilder<T>.() -> Unit = {}): NavigationResult {
    return navigator.to<T>(requireContext())
        .apply(builder)
        .go()
}

