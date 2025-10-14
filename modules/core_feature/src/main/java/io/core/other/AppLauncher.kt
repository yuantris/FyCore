@file:Suppress("IntentReset")

package io.core.other

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.core.net.toUri
import io.core.appCtx
import io.core.common.util.tools.ApkTools
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * AppLauncher 是一个用于启动应用的工具类。
 * 它提供了以下功能：
 * - 通过包名启动应用主界面。
 * - 启动指定应用的特定 Activity。
 * - 通过 URI Scheme 启动应用，支持 MIME 类型匹配。
 * - 智能启动：自动选择最佳匹配应用，支持多应用选择器。
 * - LRU缓存 Intent 解析结果以提高性能。
 * - 启动统计和性能监控。
 */
object AppLauncher {

    private const val CACHE_MAX_SIZE = 50
    private const val SYSTEM_APP_CACHE_SIZE = 200
    
    private val packageManager = appCtx.packageManager
    private val cacheLock = ReentrantReadWriteLock()
    
    // LRU缓存，自动移除最久未使用的条目
    private val resolveCache = object : LinkedHashMap<String, List<ResolveInfo>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<ResolveInfo>>?): Boolean {
            return size > CACHE_MAX_SIZE
        }
    }
    
    // 系统应用标志缓存，避免重复计算
    private val systemAppCache = ConcurrentHashMap<String, Boolean>(SYSTEM_APP_CACHE_SIZE)
    
    // 启动统计
    private val launchStats = ConcurrentHashMap<String, Int>()

    /**
     * 清除所有缓存的Intent解析结果
     */
    fun clearCache() {
        cacheLock.write {
            resolveCache.clear()
            systemAppCache.clear()
        }
    }
    
    /**
     * 获取启动统计信息
     */
    fun getLaunchStats(): Map<String, Int> = launchStats.toMap()
    
    /**
     * 清除启动统计
     */
    fun clearStats() {
        launchStats.clear()
    }

    /**
     * 通过包名启动应用主界面
     * @return 是否成功找到并尝试启动应用
     */
    fun launchAppByPackage(packageName: String): Boolean {
        if (!ApkTools.isAppInstalled(packageName)) {
            recordLaunchAttempt(packageName, false)
            return false
        }

        return safeLaunch(
            operation = {
                packageManager.getLaunchIntentForPackage(packageName)
                    ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ?.let { appCtx.startActivity(it) }
                    .let { true }
            },
            errorMessage = "Launch main failed for package: $packageName"
        ) ?: false.also { recordLaunchAttempt(packageName, false) }
    }

    /**
     * 启动指定应用的特定Activity
     * @param activityClass 完整类名（如"com.example.MainActivity"）
     */
    fun launchSpecificActivity(
        packageName: String,
        activityClass: String,
        uri: Uri? = null,
        extras: Bundle.() -> Unit = {}
    ): Boolean {
        return safeLaunch(
            operation = {
                createExplicitIntent(packageName, activityClass, uri, extras)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .let { appCtx.startActivity(it) }
                    .let { true }
            },
            errorMessage = "Launch activity failed for $packageName.$activityClass"
        ) ?: false.also { recordLaunchAttempt(packageName, false) }
    }

    /**
     * 通过URI Scheme启动应用，支持MIME类型匹配
     * @param uriString 完整的URI字符串（如"https://example.com"）
     */
    fun launchByUri(
        uriString: String,
        mimeType: String? = null,
        callback: LaunchResultCallback? = null
    ) {
        try {
            val normalizedUri = uriString.toUri().normalizeSchemeV2()
            Intent(Intent.ACTION_VIEW, normalizedUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // 修复顺序问题和类型设置方式
                setDataAndType(normalizedUri, mimeType)  // 替换原来的分开设置方式
            }.takeIf { it.resolveActivity(packageManager) != null }
                ?.let { smartLaunch(it, callback = callback) }
                ?: throw ActivityNotFoundException("No app can handle: $uriString")
        } catch (e: Exception) {
            handleException(e, callback)
        }
    }

    /**
     * 智能启动：自动选择最佳匹配应用，支持：
     * - 多应用时显示选择器
     * - 单应用时直接启动
     * - 自定义过滤规则
     */
    fun smartLaunch(
        baseIntent: Intent,
        title: String? = null,
        filter: (ResolveInfo) -> Boolean = { true },
        callback: LaunchResultCallback? = null
    ) {
        try {
            queryResolveInfo(baseIntent)
                .filter(filter)
                .sortedWith(ResolveInfoComparator)
                .takeIf { it.isNotEmpty() }
                ?.let { resolved ->
                    when (resolved.size) {
                        1 -> launchSingle(baseIntent, resolved.first(), callback)
                        else -> launchChooser(baseIntent, resolved, title, callback)
                    }
                } ?: throw ActivityNotFoundException("No apps available")
        } catch (e: Exception) {
            handleException(e, callback)
        }
    }

    /* ========================== 内部方法 ========================== */

    private fun createExplicitIntent(
        packageName: String,
        activityClass: String,
        uri: Uri? = null,
        extras: Bundle.() -> Unit = {}
    ): Intent {
        return (uri?.let {
            Intent(Intent.ACTION_VIEW, it)
        } ?: Intent()).apply {
            component = ComponentName(packageName, activityClass)
            putExtras(Bundle().apply(extras))
        }
    }

    private fun queryResolveInfo(intent: Intent): List<ResolveInfo> {
        val cacheKey = "${intent.filterHashCode()}-${intent.flags}"
        
        return cacheLock.read {
            resolveCache[cacheKey]
        } ?: cacheLock.write {
            resolveCache.getOrPut(cacheKey) {
                packageManager.queryIntentActivities(
                    intent, 
                    PackageManager.MATCH_DEFAULT_ONLY or PackageManager.MATCH_ALL
                )
            }
        }
    }

    private fun launchSingle(
        baseIntent: Intent,
        info: ResolveInfo,
        callback: LaunchResultCallback?
    ) {
        Intent(baseIntent).apply {
            component = ComponentName(info.activityInfo.packageName, info.activityInfo.name)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.also {
            logLaunchAttempt(it)
            appCtx.startActivity(it)
            recordLaunchAttempt(info.activityInfo.packageName, true)
            callback?.invoke(LaunchResult.Success)
        }
    }

    private fun launchChooser(
        baseIntent: Intent,
        resolved: List<ResolveInfo>,
        title: String?,
        callback: LaunchResultCallback?
    ) {
        val initialIntents = resolved.map { info ->
            Intent(baseIntent).apply {
                component = ComponentName(info.activityInfo.packageName, info.activityInfo.name)
                // 添加以下两行确保data和type被正确设置
                data = baseIntent.data
                type = baseIntent.type
            }
        }.toTypedArray()

        Intent.createChooser(baseIntent, title).apply {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, initialIntents)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.also {
            logLaunchAttempt(it)
            appCtx.startActivity(it)
            // 记录选择器启动，不记录具体包名
            recordLaunchAttempt("chooser", true)
            callback?.invoke(LaunchResult.Success)
        }
    }

    private fun handleException(e: Exception, callback: LaunchResultCallback? = null) {
        Log.e("AppLauncher", "Launch failed", e)
        when (e) {
            is SecurityException -> callback?.invoke(LaunchResult.PermissionDenied(e))
            is ActivityNotFoundException -> callback?.invoke(LaunchResult.AppNotFound(e))
            else -> callback?.invoke(LaunchResult.UnknownError(e))
        }
    }
    
    /**
     * 简化的异常处理包装器
     */
    private inline fun <T> safeLaunch(
        operation: () -> T,
        errorMessage: String,
        noinline callback: LaunchResultCallback? = null
    ): T? {
        return try {
            operation()
        } catch (e: Exception) {
            Log.w("AppLauncher", errorMessage, e)
            handleException(e, callback)
            null
        }
    }
    
    /**
     * 记录启动尝试
     */
    private fun recordLaunchAttempt(packageName: String, success: Boolean) {
        val key = if (success) "success_$packageName" else "failed_$packageName"
        launchStats[key] = (launchStats[key] ?: 0) + 1
    }

    private fun logLaunchAttempt(intent: Intent) {
        val childIntents = intent
            .getParcelableArrayExtra(Intent.EXTRA_INITIAL_INTENTS)
            ?.filterIsInstance<Intent>()
            .orEmpty()

        Log.d("AppLauncher", """
        |Launch Chain:
        |Chooser Intent: ${intent.action}
        |  └─ Flags: ${intent.flags}
        |  └─ Initial Intents (${childIntents.size}):
        ${
            childIntents.joinToString("\n") { child ->
                "      ├─ ${child.component} | Data: ${child.data} | Type: ${child.type}"
            }
        }
    """.trimMargin())
    }

    /* ========================== 工具方法 ========================== */
    @JvmStatic
    fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    fun Context.getInstalledApps(filter: (ResolveInfo) -> Boolean = { true }): List<ResolveInfo> {
        return packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            PackageManager.MATCH_ALL
        ).filter(filter)
    }

    private fun Uri.normalizeSchemeV2(): Uri {
        return when (val scheme = scheme?.lowercase()) {
            "file" -> normalizeFileUri()
            "http", "https" -> this
            null -> buildUpon().scheme("content").build()
            else -> if (isOpaque) this else buildUpon().scheme(scheme).build()
        }
    }
    
    private fun Uri.normalizeFileUri(): Uri {
        return if (path?.contains(File.separator) == true) {
            this
        } else {
            try {
                "file://$this".toUri()
            } catch (e: Exception) {
                Log.w("AppLauncher", "Failed to normalize file URI: $this", e)
                this
            }
        }
    }

    /* ========================== 内部对象 ========================== */

    private object ResolveInfoComparator : Comparator<ResolveInfo> {
        override fun compare(a: ResolveInfo, b: ResolveInfo): Int {
            val aSystem = a.isSystemApp
            val bSystem = b.isSystemApp
            return when {
                aSystem && !bSystem -> -1
                !aSystem && bSystem -> 1
                else -> b.priority - a.priority
            }
        }

        private val ResolveInfo.isSystemApp: Boolean
            get() = systemAppCache.getOrPut(activityInfo.packageName) {
                (activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            }
    }
}

typealias LaunchResultCallback = (result: LaunchResult) -> Unit

sealed class LaunchResult {
    object Success : LaunchResult()
    data class AppNotFound(val exception: Exception) : LaunchResult()
    data class PermissionDenied(val exception: Exception) : LaunchResult()
    data class UnknownError(val exception: Exception) : LaunchResult()
}