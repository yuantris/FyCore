package io.core.nav

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.core.nav.navigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlin.reflect.KClass

/**
 * 生产级导航库使用示例
 * 现在使用 NavigationManager 单例模式
 */
object NavigationExample {

    /**
     * 方式2：自定义配置初始化（高级用法）
     */
    fun initializeWithCustomConfig() {
        NavigationManager.initialize {
            // 自定义执行器和监听器
            setExecutor(ProductionNavigationExecutor())
            setListener(ProductionNavigationListener())
            
            // 权限检查拦截器
            addInterceptor(
                PermissionInterceptor(
                    permissionChecker = AndroidPermissionChecker(),
                    permissionRequirements = mapOf(
                        CameraActivity::class.java.name to listOf("android.permission.CAMERA"),
                        LocationActivity::class.java.name to listOf(
                            "android.permission.ACCESS_FINE_LOCATION",
                            "android.permission.ACCESS_COARSE_LOCATION"
                        )
                    ),
                    priority = 50
                )
            )
            
            // 登录拦截器
            addInterceptor(
                LoginInterceptor(
                    authProvider = AppAuthProvider(),
                    loginActivityClass = LoginActivity::class,
                    protectedActivities = setOf(
                        ProfileActivity::class,
                        SettingsActivity::class,
                        OrderActivity::class
                    ),
                    priority = 100
                )
            )
            
            // A/B测试拦截器
            addInterceptor(
                ABTestInterceptor(
                    abTestProvider = AppABTestProvider(),
                    experiments = mapOf(
                        "home_redesign" to ABTestConfig(
                            targetActivities = setOf(HomeActivity::class),
                            variants = mapOf(
                                "control" to HomeActivity::class,
                                "variant_a" to HomeActivityV2::class
                            )
                        )
                    ),
                    priority = 150
                )
            )
            
            // 埋点拦截器
            addInterceptor(
                AnalyticsInterceptor(
                    analytics = AppAnalytics(),
                    priority = 200
                )
            )
            
            // 注册路由（可选）
            registerRoute<HomeActivity>("home")
            registerRoute<ProfileActivity>("profile")
            registerRoute<SettingsActivity>("settings")
            registerRoute<LoginActivity>("login")
            registerRoute<OrderActivity>("order")
            registerRoute<CameraActivity>("camera")
            registerRoute<LocationActivity>("location")
        }
    }
}

/**
 * 生产级导航执行器
 */
class ProductionNavigationExecutor : NavigationExecutor {
    override fun execute(context: Context, result: NavigationResult) {
        when (result) {
            is NavigationResult.Proceed -> {
                try {
                    context.startActivity(result.intent)
                } catch (e: Exception) {
                    // 处理 startActivity 异常
                    CrashReporter.logException("Navigation failed", e)
                }
            }
            is NavigationResult.Redirect -> {
                try {
                    context.startActivity(result.intent)
                } catch (e: Exception) {
                    CrashReporter.logException("Navigation redirect failed", e)
                }
            }
            is NavigationResult.Abort -> {
                // 可以显示用户友好的提示
                if (result.reason.contains("权限")) {
                    showPermissionDialog(context)
                } else if (result.reason.contains("登录")) {
                    // 已经被拦截器处理了
                } else {
                    showGenericError(context, result.reason)
                }
            }
            is NavigationResult.Error -> {
                CrashReporter.logException("Navigation error", result.exception)
                showGenericError(context, "页面跳转失败")
            }
        }
    }

    private fun showPermissionDialog(context: Context) {
        // 显示权限说明对话框
    }

    private fun showGenericError(context: Context, message: String) {
        // 显示通用错误提示
    }
}

/**
 * 生产级导航监听器
 */
class ProductionNavigationListener : NavigationListener {
    override fun onNavigationStart(request: NavigationRequest) {
        Logger.d("Navigation", "Starting navigation to ${request.intent.component?.className}")
    }

    override fun onNavigationResult(result: NavigationResult) {
        when (result) {
            is NavigationResult.Proceed -> {
                Logger.d("Navigation", "Navigation succeeded")
            }
            is NavigationResult.Redirect -> {
                Logger.d("Navigation", "Navigation redirected: ${result.reason}")
            }
            is NavigationResult.Abort -> {
                Logger.w("Navigation", "Navigation aborted: ${result.reason}")
            }
            is NavigationResult.Error -> {
                Logger.e("Navigation", "Navigation error", result.exception)
            }
        }
    }

    override fun onInterceptorExecute(interceptor: NavigationInterceptor, request: NavigationRequest) {
        Logger.d("Navigation", "Executing interceptor: ${interceptor.name}")
    }
}

// ==================== 实际使用示例 ====================

/**
 * 在 Activity 中的使用示例
 */
class MainActivity : Activity() {

    private fun navigateToProfile() {
        // 方式1：通过NavigationManager获取导航器（推荐）
        NavigationManager.getNavigator()
            .to<ProfileActivity>(this)
            .with("userId", "123")
            .with("source", "main_page")
            .go()
    }

    private fun navigateToSettings() {
        // 方式2：使用Context扩展函数（更简洁）
        navigator
            .to<SettingsActivity>(this)
            .with("section", "privacy")
            .go()
    }

    private fun navigateWithCallback() {
        // 方式3：异步导航带回调
        NavigationManager.getNavigator()
            .to<OrderActivity>(this)
            .with("orderId", "ORDER_123")
            .goAsync { result ->
                when (result) {
                    is NavigationResult.Proceed -> {
                        // 导航成功
                        hideLoading()
                    }
                    is NavigationResult.Redirect -> {
                        // 被重定向（比如跳转到登录页）
                        showMessage("需要先登录")
                    }
                    is NavigationResult.Abort -> {
                        // 导航被取消
                        showMessage(result.reason)
                    }
                    is NavigationResult.Error -> {
                        // 导航出错
                        showError("跳转失败")
                    }
                }
            }
    }

    private fun navigateWithFlags() {
        // 方式4：带 Intent flags
        navigator
            .to<HomeActivity>(this)
            .flags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            .go()
    }

    private fun navigateByRoute() {
        // 方式5：使用路由字符串（如果注册了路由）
        navigator
            .to(this, "profile")
            .with("userId", "123")
            .go()
    }

    private fun hideLoading() {}
    private fun showMessage(message: String) {}
    private fun showError(message: String) {}
}

/**
 * 登录页面使用示例
 */
class LoginActivity : Activity() {

    private fun onLoginSuccess() {
        // 登录成功后自动回跳到原目标页面
        LoginDispatcher.dispatchAfterLogin(
            activity = this,
            navigator = NavigationManager.getNavigator()
        ) {
            // 兜底操作：没有回跳信息时跳转到首页
            navigator.to<HomeActivity>(this).go()
        }
    }

    private fun checkPendingNavigation() {
        if (LoginDispatcher.hasPendingNavigation(this)) {
            val reason = LoginDispatcher.getRedirectReason(this)
            showMessage("$reason，请先登录")
        }
    }

    private fun showMessage(message: String) {}
}

// ==================== 接口实现示例 ====================

class AppAuthProvider : AuthProvider {
    override suspend fun isLoggedIn(): Boolean {
        // 异步检查登录状态（比如验证 token）
        return TokenManager.validateToken()
    }

    override fun isLoggedInSync(): Boolean {
        // 同步检查登录状态（比如检查本地缓存）
        return TokenManager.hasValidLocalToken()
    }
}

class AndroidPermissionChecker : PermissionChecker {
    override suspend fun hasPermissions(context: Context, permissions: List<String>): Boolean {
        return permissions.all { 
            context.checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED 
        }
    }

    override fun hasPermissionsSync(context: Context, permissions: List<String>): Boolean {
        return permissions.all { 
            context.checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED 
        }
    }
}

class AppAnalytics : Analytics {
    override fun trackNavigation(targetActivity: String, extras: Bundle?) {
        // 发送埋点数据
        val params = mutableMapOf<String, Any>()
        params["target_activity"] = targetActivity
        extras?.keySet()?.forEach { key ->
            extras.get(key)?.let { value ->
                params["param_$key"] = value
            }
        }
        AnalyticsSDK.track("page_navigation", params)
    }
}

class AppABTestProvider : ABTestProvider {
    override suspend fun getVariant(experimentKey: String): String? {
        return ABTestSDK.getVariantAsync(experimentKey)
    }

    override fun getCachedVariant(experimentKey: String): String? {
        return ABTestSDK.getCachedVariant(experimentKey)
    }
}

// ==================== 示例 Activity 类 ====================

class HomeActivity : Activity()
class HomeActivityV2 : Activity() // A/B 测试变体
class ProfileActivity : Activity()
class SettingsActivity : Activity()
class OrderActivity : Activity()
class CameraActivity : Activity()
class LocationActivity : Activity()

// ==================== 工具类 ====================

object TokenManager {
    suspend fun validateToken(): Boolean = false
    fun hasValidLocalToken(): Boolean = true
}

object CrashReporter {
    fun logException(message: String, exception: Exception) {}
}

object Logger {
    fun d(tag: String, message: String) {}
    fun w(tag: String, message: String) {}
    fun e(tag: String, message: String, exception: Exception? = null) {}
}

object AnalyticsSDK {
    fun track(event: String, params: Map<String, Any>) {}
}

object ABTestSDK {
    suspend fun getVariantAsync(experimentKey: String): String? = null
    fun getCachedVariant(experimentKey: String): String? = null
}