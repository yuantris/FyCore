package io.core.nav

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.*
import kotlin.reflect.KClass

/**
 * 生产级导航器 - 简化参数，增强泛型支持
 */
class Navigator private constructor(
    private var interceptors: List<NavigationInterceptor>,
    private var executor: NavigationExecutor,
    private var listener: NavigationListener?,
    internal val routeRegistry: RouteRegistry,
    private val scope: CoroutineScope
) {

    companion object {
        fun builder(): Builder = Builder()
    }

    /**
     * Builder 模式构建器
     */
    class Builder {
        private val interceptors = mutableListOf<NavigationInterceptor>()
        private var executor: NavigationExecutor = DefaultNavigationExecutor()
        private var listener: NavigationListener? = null
        private val routeRegistry = RouteRegistry()
        @OptIn(DelicateCoroutinesApi::class)
        private var scope: CoroutineScope = GlobalScope

        fun addInterceptor(interceptor: NavigationInterceptor): Builder {
            interceptors.add(interceptor)
            return this
        }

        fun setExecutor(executor: NavigationExecutor): Builder {
            this.executor = executor
            return this
        }

        fun setListener(listener: NavigationListener): Builder {
            this.listener = listener
            return this
        }

        fun setCoroutineScope(scope: CoroutineScope): Builder {
            this.scope = scope
            return this
        }


        inline fun <reified T : Activity> registerRoute(route: String): Builder {
            return registerRoute(route, T::class)
        }

        fun <T : Activity> registerRoute(route: String, activityClass: KClass<T>): Builder {
            routeRegistry.register(route, activityClass)
            return this
        }

        fun build(): Navigator {
            val sortedInterceptors = interceptors.sortedBy { it.priority }
            return Navigator(sortedInterceptors, executor, listener, routeRegistry, scope)
        }
    }

    // ==================== 核心导航方法 ====================

    /**
     * 使用泛型的类型安全导航
     */
    inline fun <reified T : Activity> to(context: Context): NavigationBuilder<T> {
        return NavigationBuilder(context, T::class, this)
    }

    /**
     * 使用路由字符串导航
     */
    fun to(context: Context, route: String): RouteNavigationBuilder {
        return RouteNavigationBuilder(context, route, this)
    }

    /**
     * 直接使用 Intent 导航
     */
    fun navigate(context: Context, intent: Intent): NavigationResult {
        val request = NavigationRequest(context, intent)
        return executeNavigation(request)
    }

    /**
     * 异步导航
     */
    fun navigateAsync(
        context: Context, 
        intent: Intent, 
        callback: (NavigationResult) -> Unit
    ) {
        scope.launch {
            val request = NavigationRequest(context, intent)
            val result = executeNavigationSuspend(request)
            withContext(Dispatchers.Main) {
                callback(result)
            }
        }
    }

    // ==================== 内部执行方法 ====================

    internal fun executeNavigation(request: NavigationRequest): NavigationResult {
        listener?.onNavigationStart(request)
        
        val chain = InterceptorChain(interceptors, 0, scope, listener)
        val result = chain.proceed(request)
        
        listener?.onNavigationResult(result)
        executor.execute(request.context, result)
        
        return result
    }

    internal suspend fun executeNavigationSuspend(request: NavigationRequest): NavigationResult {
        listener?.onNavigationStart(request)
        
        val chain = InterceptorChain(interceptors, 0, scope, listener)
        val result = chain.proceedSuspend(request)
        
        listener?.onNavigationResult(result)
        withContext(Dispatchers.Main) {
            executor.execute(request.context, result)
        }
        
        return result
    }

    fun resolveRoute(route: String): KClass<out Activity>? {
        return routeRegistry.resolve(route)
    }

    fun createRouteIntent(context: Context, route: String): Intent? {
        return routeRegistry.createIntent(context, route)
    }

    // ==================== 配置访问方法 ====================

    /**
     * 获取当前配置的拦截器列表（只读）
     */
    fun getInterceptors(): List<NavigationInterceptor> = interceptors.toList()

    /**
     * 获取当前配置的执行器
     */
    fun getExecutor(): NavigationExecutor = executor

    /**
     * 获取当前配置的监听器
     */
    fun getListener(): NavigationListener? = listener

    // ==================== 动态配置方法 ====================

    /**
     * 动态添加拦截器（运行时）
     * @param interceptor 要添加的拦截器
     * @param priority 优先级，默认为 0。数值越小越先执行
     * @return 当前 Navigator 实例，支持链式调用
     */
    fun addInterceptor(interceptor: NavigationInterceptor, priority: Int = 0): Navigator {
        synchronized(this) {
            // 创建一个包装类来临时存储优先级
            val interceptorWithPriority = object : NavigationInterceptor {
                override val priority: Int = priority
                override val name: String = interceptor.name
                override fun intercept(request: NavigationRequest, chain: InterceptorChain): NavigationResult {
                    return interceptor.intercept(request, chain)
                }
                override suspend fun interceptSuspend(request: NavigationRequest, chain: InterceptorChain): NavigationResult {
                    return interceptor.interceptSuspend(request, chain)
                }
            }
            
            val newInterceptors = interceptors.toMutableList()
            newInterceptors.add(interceptorWithPriority)
            // 按优先级重新排序（数值越小越先执行）
            interceptors = newInterceptors.sortedBy { it.priority }
        }
        return this
    }

    /**
     * 动态移除拦截器（运行时）
     * @param interceptor 要移除的拦截器实例
     * @return 当前 Navigator 实例，支持链式调用
     */
    fun removeInterceptor(interceptor: NavigationInterceptor): Navigator {
        synchronized(this) {
            interceptors = interceptors.filter { it !== interceptor }
        }
        return this
    }

    /**
     * 动态移除指定类型的拦截器（运行时）
     * @param interceptorClass 要移除的拦截器类型
     * @return 当前 Navigator 实例，支持链式调用
     */
    fun removeInterceptor(interceptorClass: Class<out NavigationInterceptor>): Navigator {
        synchronized(this) {
            interceptors = interceptors.filter { !interceptorClass.isInstance(it) }
        }
        return this
    }

    /**
     * 动态设置执行器（运行时）
     * @param executor 新的执行器
     * @return 当前 Navigator 实例，支持链式调用
     */
    fun setExecutor(executor: NavigationExecutor): Navigator {
        synchronized(this) {
            this.executor = executor
        }
        return this
    }

    /**
     * 动态设置监听器（运行时）
     * @param listener 新的监听器，null 表示移除监听器
     * @return 当前 Navigator 实例，支持链式调用
     */
    fun setListener(listener: NavigationListener?): Navigator {
        synchronized(this) {
            this.listener = listener
        }
        return this
    }

    /**
     * 动态添加路由（运行时）
     * @param route 路由路径
     * @param targetClass 目标 Activity 类
     * @return 当前 Navigator 实例，支持链式调用
     */
    fun addRoute(route: String, targetClass: Class<out Activity>): Navigator {
        synchronized(this) {
            routeRegistry.register(route, targetClass.kotlin)
        }
        return this
    }

    /**
     * 动态移除路由（运行时）
     * @param route 要移除的路由路径
     * @return 当前 Navigator 实例，支持链式调用
     */
    fun removeRoute(route: String): Navigator {
        synchronized(this) {
            routeRegistry.unregister(route)
        }
        return this
    }
}

/**
 * 类型安全的导航构建器
 */
class NavigationBuilder<T : Activity>(
    private val context: Context,
    private val activityClass: KClass<T>,
    private val navigator: Navigator
) {
    private var extras: Bundle? = null
    private var flags: Int? = null
    private var action: String? = null

    fun with(key: String, value: String): NavigationBuilder<T> {
        ensureExtras().putString(key, value)
        return this
    }

    fun with(key: String, value: Int): NavigationBuilder<T> {
        ensureExtras().putInt(key, value)
        return this
    }

    fun with(key: String, value: Boolean): NavigationBuilder<T> {
        ensureExtras().putBoolean(key, value)
        return this
    }

    fun with(bundle: Bundle): NavigationBuilder<T> {
        ensureExtras().putAll(bundle)
        return this
    }

    fun flags(flags: Int): NavigationBuilder<T> {
        this.flags = flags
        return this
    }

    fun action(action: String): NavigationBuilder<T> {
        this.action = action
        return this
    }

    fun go(): NavigationResult {
        val intent = createIntent()
        val request = NavigationRequest(context, intent)
        return navigator.executeNavigation(request)
    }

    fun goAsync(callback: (NavigationResult) -> Unit) {
        val intent = createIntent()
        navigator.navigateAsync(context, intent, callback)
    }

    private fun createIntent(): Intent {
        return Intent(context, activityClass.java).apply {
            this@NavigationBuilder.extras?.let { putExtras(it) }
            setFlags(flags)
            action?.let { setAction(it) }
        }
    }

    private fun ensureExtras(): Bundle {
        if (extras == null) {
            extras = Bundle()
        }
        return extras!!
    }
}

/**
 * 路由导航构建器
 */
class RouteNavigationBuilder(
    private val context: Context,
    private val route: String,
    private val navigator: Navigator
) {
    private var extras: Bundle? = null
    private var flags: Int? = null

    fun with(key: String, value: String): RouteNavigationBuilder {
        ensureExtras().putString(key, value)
        return this
    }

    fun with(key: String, value: Int): RouteNavigationBuilder {
        ensureExtras().putInt(key, value)
        return this
    }

    fun with(bundle: Bundle): RouteNavigationBuilder {
        ensureExtras().putAll(bundle)
        return this
    }

    fun flags(flags: Int): RouteNavigationBuilder {
        this.flags = flags
        return this
    }

    fun go(): NavigationResult {
        val intent = createIntent() ?: return NavigationResult.Abort(
            "Route not found: $route",
            Intent() // 空 Intent 作为原始意图
        )
        val request = NavigationRequest(context, intent)
        return navigator.executeNavigation(request)
    }

    fun goAsync(callback: (NavigationResult) -> Unit) {
        val intent = createIntent()
        if (intent == null) {
            callback(NavigationResult.Abort("Route not found: $route", Intent()))
            return
        }
        navigator.navigateAsync(context, intent, callback)
    }

    private fun createIntent(): Intent? {
        return navigator.createRouteIntent(context, route)?.apply {
            this@RouteNavigationBuilder.extras?.let { putExtras(it) }
            setFlags(flags)
        }
    }

    private fun ensureExtras(): Bundle {
        if (extras == null) {
            extras = Bundle()
        }
        return extras!!
    }
}