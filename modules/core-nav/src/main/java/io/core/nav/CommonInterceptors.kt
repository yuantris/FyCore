package io.core.nav

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass

/**
 * 生产级登录拦截器
 */
class LoginInterceptor(
    private val authProvider: AuthProvider,
    private val loginActivityClass: KClass<out Activity>, // 直接使用Activity Class
    private val protectedRoutes: Set<String> = emptySet(),
    private val protectedActivities: Set<KClass<out Activity>> = emptySet(),
    override val priority: Int = 100
) : NavigationInterceptor {

    override val name: String = "LoginInterceptor"

    override fun intercept(request: NavigationRequest, chain: InterceptorChain): NavigationResult {
        // 同步检查（适用于本地缓存的登录状态）
        if (authProvider.isLoggedInSync() || !needsLogin(request)) {
            return chain.proceed(request)
        }

        return createLoginRedirect(request)
    }

    override suspend fun interceptSuspend(request: NavigationRequest, chain: InterceptorChain): NavigationResult {
        // 异步检查（适用于需要网络验证的场景�?
        val isLoggedIn = withContext(Dispatchers.IO) {
            authProvider.isLoggedIn()
        }

        if (isLoggedIn || !needsLogin(request)) {
            return chain.proceedSuspend(request)
        }

        return createLoginRedirect(request)
    }

    private fun needsLogin(request: NavigationRequest): Boolean {
        val targetClassName = request.intent.component?.className
        
        // 检查是否在保护�?Activity 列表�?
        val isProtectedActivity = protectedActivities.any { 
            it.java.name == targetClassName 
        }
        
        // 检查是否在保护的路由列表中（通过 metadata 传递）
        val currentRoute = request.metadata.getString("route")
        val isProtectedRoute = currentRoute != null && protectedRoutes.contains(currentRoute)
        
        return isProtectedActivity || isProtectedRoute
    }

    private fun createLoginRedirect(request: NavigationRequest): NavigationResult {
        // 直接使用Activity Class创建Intent
        val loginIntent = Intent(request.context, loginActivityClass.java).apply {
            // 保存原始导航信息
            putExtra(LoginKeys.ORIGINAL_INTENT, request.originalIntent)
            putExtra(LoginKeys.ORIGINAL_EXTRAS, request.intent.extras)
            putExtra(LoginKeys.REDIRECT_REASON, "需要登�?)
        }

        return NavigationResult.Redirect(
            intent = loginIntent,
            originalIntent = request.originalIntent,
            reason = "用户未登录，重定向到登录�?
        )
    }
}

/**
 * 权限检查拦截器
 */
class PermissionInterceptor(
    private val permissionChecker: PermissionChecker,
    private val permissionRequirements: Map<String, List<String>>,
    override val priority: Int = 50
) : NavigationInterceptor {

    override val name: String = "PermissionInterceptor"

    override suspend fun interceptSuspend(request: NavigationRequest, chain: InterceptorChain): NavigationResult {
        val targetClassName = request.intent.component?.className
        val requiredPermissions = permissionRequirements[targetClassName]

        if (requiredPermissions != null) {
            val hasPermissions = withContext(Dispatchers.Main) {
                permissionChecker.hasPermissions(request.context, requiredPermissions)
            }

            if (!hasPermissions) {
                return NavigationResult.Abort(
                    reason = "缺少必要权限: ${requiredPermissions.joinToString()}",
                    originalIntent = request.originalIntent
                )
            }
        }

        return chain.proceedSuspend(request)
    }

    override fun intercept(request: NavigationRequest, chain: InterceptorChain): NavigationResult {
        // 同步版本：假设权限检查是同步�?
        val targetClassName = request.intent.component?.className
        val requiredPermissions = permissionRequirements[targetClassName]

        if (requiredPermissions != null) {
            val hasPermissions = permissionChecker.hasPermissionsSync(request.context, requiredPermissions)
            if (!hasPermissions) {
                return NavigationResult.Abort(
                    reason = "缺少必要权限: ${requiredPermissions.joinToString()}",
                    originalIntent = request.originalIntent
                )
            }
        }

        return chain.proceed(request)
    }
}

/**
 * 埋点拦截�?
 */
class AnalyticsInterceptor(
    private val analytics: Analytics,
    override val priority: Int = 200
) : NavigationInterceptor {

    override val name: String = "AnalyticsInterceptor"

    override fun intercept(request: NavigationRequest, chain: InterceptorChain): NavigationResult {
        // 记录导航事件
        val targetActivity = request.intent.component?.className ?: "Unknown"
        analytics.trackNavigation(targetActivity, request.intent.extras)

        return chain.proceed(request)
    }
}

/**
 * A/B 测试拦截�?
 */
class ABTestInterceptor(
    private val abTestProvider: ABTestProvider,
    private val experiments: Map<String, ABTestConfig>,
    override val priority: Int = 150
) : NavigationInterceptor {

    override val name: String = "ABTestInterceptor"

    override suspend fun interceptSuspend(request: NavigationRequest, chain: InterceptorChain): NavigationResult {
        val targetClassName = request.intent.component?.className
        
        experiments.forEach { (experimentKey, config) ->
            if (config.targetActivities.any { it.java.name == targetClassName }) {
                val variant = withContext(Dispatchers.IO) {
                    abTestProvider.getVariant(experimentKey)
                }
                
                val alternativeActivity = config.variants[variant]
                if (alternativeActivity != null && alternativeActivity.java.name != targetClassName) {
                    val newIntent = Intent(request.context, alternativeActivity.java).apply {
                        putExtras(request.intent.extras ?: Bundle())
                        action = request.intent.action
                        data = request.intent.data
                        flags = request.intent.flags
                    }
                    
                    return chain.proceedSuspend(request.withIntent(newIntent))
                }
            }
        }
        
        return chain.proceedSuspend(request)
    }

    override fun intercept(request: NavigationRequest, chain: InterceptorChain): NavigationResult {
        // 同步版本使用缓存�?A/B 测试结果
        val targetClassName = request.intent.component?.className
        
        experiments.forEach { (experimentKey, config) ->
            if (config.targetActivities.any { it.java.name == targetClassName }) {
                val variant = abTestProvider.getCachedVariant(experimentKey)
                val alternativeActivity = config.variants[variant]
                
                if (alternativeActivity != null && alternativeActivity.java.name != targetClassName) {
                    val newIntent = Intent(request.context, alternativeActivity.java).apply {
                        putExtras(request.intent.extras ?: Bundle())
                        action = request.intent.action
                        data = request.intent.data
                        flags = request.intent.flags
                    }
                    
                    return chain.proceed(request.withIntent(newIntent))
                }
            }
        }
        
        return chain.proceed(request)
    }
}

// ==================== 接口定义 ====================

/**
 * 认证提供者接�?
 */
interface AuthProvider {
    /**
     * 同步检查登录状态（默认实现基于异步方法�?
     */
    fun isLoggedInSync(): Boolean = runBlocking { isLoggedIn() }
    
    /**
     * 异步检查登录状态（必须实现�?
     */
    suspend fun isLoggedIn(): Boolean
}

/**
 * 权限检查器接口
 */
interface PermissionChecker {
    suspend fun hasPermissions(context: Context, permissions: List<String>): Boolean
    fun hasPermissionsSync(context: Context, permissions: List<String>): Boolean
}

/**
 * 埋点接口
 */
interface Analytics {
    fun trackNavigation(targetActivity: String, extras: Bundle?)
}

/**
 * A/B 测试提供者接�?
 */
interface ABTestProvider {
    suspend fun getVariant(experimentKey: String): String?
    fun getCachedVariant(experimentKey: String): String?
}

/**
 * A/B 测试配置
 */
data class ABTestConfig(
    val targetActivities: Set<KClass<out Activity>>,
    val variants: Map<String, KClass<out Activity>>
)

/**
 * 登录相关常量
 */
object LoginKeys {
    const val ORIGINAL_INTENT = "io.core.nav.ORIGINAL_INTENT"
    const val ORIGINAL_EXTRAS = "io.core.nav.ORIGINAL_EXTRAS"
    const val REDIRECT_REASON = "io.core.nav.REDIRECT_REASON"
}