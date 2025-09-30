package io.core.nav

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.*
import kotlin.reflect.KClass

/**
 * 生产级导航核心接口
 */
interface NavigationInterceptor {
    val priority: Int get() = 0
    val name: String get() = this::class.java.simpleName

    /**
     * 同步拦截
     */
    fun intercept(request: NavigationRequest, chain: InterceptorChain): NavigationResult

    /**
     * 异步拦截（可选实现）
     */
    suspend fun interceptSuspend(request: NavigationRequest, chain: InterceptorChain): NavigationResult {
        return intercept(request, chain)
    }
}

/**
 * 导航请求封装
 */
data class NavigationRequest(
    val context: Context,
    val intent: Intent,
    val originalIntent: Intent = intent, // 保留原始意图
    val metadata: Bundle = Bundle()
) {
    inline fun <reified T : Activity> targetClass(): KClass<T>? {
        return intent.component?.className?.let { className ->
            try {
                val clazz = Class.forName(className)
                if (T::class.java.isAssignableFrom(clazz)) {
                    @Suppress("UNCHECKED_CAST")
                    clazz.kotlin as KClass<T>
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    fun withIntent(newIntent: Intent): NavigationRequest {
        return copy(intent = newIntent)
    }

    fun withMetadata(key: String, value: Any): NavigationRequest {
        metadata.putString(key, value.toString())
        return this
    }
}

/**
 * 拦截器链
 */
class InterceptorChain internal constructor(
    private val interceptors: List<NavigationInterceptor>,
    private val index: Int,
    private val scope: CoroutineScope,
    private val listener: NavigationListener?
) {
    fun proceed(request: NavigationRequest): NavigationResult {
        return if (index < interceptors.size) {
            val interceptor = interceptors[index]
            val next = InterceptorChain(interceptors, index + 1, scope, listener)
            try {
                val result = interceptor.intercept(request, next)
                listener?.onInterceptorExecute(interceptor, request, InterceptorResult.Intercepted(result))
                result
            } catch (e: Exception) {
                val errorResult = NavigationResult.Error(e, request.originalIntent)
                listener?.onInterceptorExecute(interceptor, request, InterceptorResult.Intercepted(errorResult))
                errorResult
            }
        } else {
            NavigationResult.Proceed(request.intent, request.originalIntent)
        }
    }

    suspend fun proceedSuspend(request: NavigationRequest): NavigationResult {
        return if (index < interceptors.size) {
            val interceptor = interceptors[index]
            val next = InterceptorChain(interceptors, index + 1, scope, listener)
            try {
                val result = interceptor.interceptSuspend(request, next)
                listener?.onInterceptorExecute(interceptor, request, InterceptorResult.Intercepted(result))
                result
            } catch (e: Exception) {
                val errorResult = NavigationResult.Error(e, request.originalIntent)
                listener?.onInterceptorExecute(interceptor, request, InterceptorResult.Intercepted(errorResult))
                errorResult
            }
        } else {
            NavigationResult.Proceed(request.intent, request.originalIntent)
        }
    }
}

/**
 * 导航结果
 */
sealed class NavigationResult {
    abstract val originalIntent: Intent

    data class Proceed(
        val intent: Intent,
        override val originalIntent: Intent
    ) : NavigationResult()

    data class Redirect(
        val intent: Intent,
        override val originalIntent: Intent,
        val reason: String? = null
    ) : NavigationResult()

    data class Abort(
        val reason: String,
        override val originalIntent: Intent
    ) : NavigationResult()

    data class Error(
        val exception: Exception,
        override val originalIntent: Intent
    ) : NavigationResult()
}

/**
 * 导航执行器接口
 */
interface NavigationExecutor {
    fun execute(context: Context, result: NavigationResult)
}

/**
 * 默认导航执行器
 */
class DefaultNavigationExecutor : NavigationExecutor {
    override fun execute(context: Context, result: NavigationResult) {
        when (result) {
            is NavigationResult.Proceed -> {
                context.startActivity(result.intent)
            }
            is NavigationResult.Redirect -> {
                context.startActivity(result.intent)
            }
            is NavigationResult.Abort -> {
                // 可通过监听器处理
            }
            is NavigationResult.Error -> {
                // 可通过监听器处理
            }
        }
    }
}

/**
 * 拦截器执行结果
 */
sealed class InterceptorResult {
    object Proceed : InterceptorResult()
    data class Intercepted(val result: NavigationResult) : InterceptorResult()
}

/**
 * 导航监听器
 */
interface NavigationListener {
    fun onNavigationStart(request: NavigationRequest) {}
    fun onNavigationResult(result: NavigationResult) {}
    fun onInterceptorExecute(
        interceptor: NavigationInterceptor,
        request: NavigationRequest,
        result: InterceptorResult
    ) {}
}

/**
 * 路由注册表
 */
class RouteRegistry {
    private val routes = mutableMapOf<String, KClass<out Activity>>()

    fun <T : Activity> register(route: String, activityClass: KClass<T>): RouteRegistry {
        routes[route] = activityClass
        return this
    }

    inline fun <reified T : Activity> register(route: String): RouteRegistry {
        return register(route, T::class)
    }

    fun resolve(route: String): KClass<out Activity>? = routes[route]

    fun createIntent(context: Context, route: String): Intent? {
        return resolve(route)?.let { Intent(context, it.java) }
    }

    /**
     * 注销路由
     * @param route 要移除的路由路径
     * @return 当前 RouteRegistry 实例，支持链式调用
     */
    fun unregister(route: String): RouteRegistry {
        routes.remove(route)
        return this
    }

    /**
     * 清空所有路由
     * @return 当前 RouteRegistry 实例，支持链式调用
     */
    fun clear(): RouteRegistry {
        routes.clear()
        return this
    }

    /**
     * 获取所有已注册的路由
     * @return 路由映射的只读副本
     */
    fun getAllRoutes(): Map<String, KClass<out Activity>> = routes.toMap()

    /**
     * 检查路由是否已注册
     * @param route 路由路径
     * @return 是否已注册
     */
    fun isRegistered(route: String): Boolean = routes.containsKey(route)
}