package io.core.nav

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * 生产级登录后回跳工具 - 类型安全，无反射
 */
object LoginDispatcher {

    /**
     * 登录成功后回跳到原目标页�?
     * 
     * @param activity 当前登录页面
     * @param navigator 导航器实�?
     * @param fallbackAction 没有回跳信息时的兜底操作
     */
    fun dispatchAfterLogin(
        activity: Activity,
        navigator: Navigator,
        fallbackAction: (() -> Unit)? = null
    ) {
        val originalIntent = activity.intent.getParcelableExtra<Intent>(LoginKeys.ORIGINAL_INTENT)
        val originalExtras = activity.intent.getBundleExtra(LoginKeys.ORIGINAL_EXTRAS)

        if (originalIntent != null) {
            // 恢复原始 Intent �?extras
            originalExtras?.let { originalIntent.putExtras(it) }
            
            // 使用导航器执行跳�?
            val result = navigator.navigate(activity, originalIntent)
            
            when (result) {
                is NavigationResult.Proceed,
                is NavigationResult.Redirect -> {
                    activity.finish()
                }
                is NavigationResult.Abort,
                is NavigationResult.Error -> {
                    // 回跳失败，执行兜底操�?
                    fallbackAction?.invoke() ?: activity.finish()
                }
            }
        } else {
            // 没有原始 Intent，执行兜底操�?
            fallbackAction?.invoke() ?: activity.finish()
        }
    }

    /**
     * 使用路由的回跳方�?
     */
    fun dispatchAfterLoginByRoute(
        activity: Activity,
        navigator: Navigator,
        fallbackRoute: String? = null
    ) {
        val originalIntent = activity.intent.getParcelableExtra<Intent>(LoginKeys.ORIGINAL_INTENT)
        
        if (originalIntent != null) {
            val result = navigator.navigate(activity, originalIntent)
            if (result is NavigationResult.Proceed || result is NavigationResult.Redirect) {
                activity.finish()
                return
            }
        }
        
        // 原始跳转失败或不存在，使用兜底路�?
        if (fallbackRoute != null) {
            navigator.to(activity, fallbackRoute).go()
        }
        activity.finish()
    }

    /**
     * 检查是否有待回跳的页面
     */
    fun hasPendingNavigation(activity: Activity): Boolean {
        return activity.intent.hasExtra(LoginKeys.ORIGINAL_INTENT)
    }

    /**
     * 获取回跳原因
     */
    fun getRedirectReason(activity: Activity): String? {
        return activity.intent.getStringExtra(LoginKeys.REDIRECT_REASON)
    }
}