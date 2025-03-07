@file:Suppress("unused", "UnusedReceiverParameter", "DEPRECATION")

package io.core.common.util.extensions.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.PendingIntent.getActivity
import android.app.PendingIntent.getBroadcast
import android.app.PendingIntent.getService
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import io.core.R
import io.core.common.base.component.dialog.CustomToast
import io.core.common.util.Toaster
import io.core.common.util.extensions.cool.logPrint
import io.core.common.util.extensions.cool.printOnDebug
import io.core.common.util.extensions.cool.pxToDp
import io.core.common.util.extensions.layoutInflater
import io.core.common.util.extensions.windowManager
import io.core.common.util.tools.Preferences
import kotlin.system.exitProcess

val Context.ctx
    get() = getActivity()

/**
 * @return 上下文中的Activity对象
 */
private fun Context.getActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
}

/**
 * 判断当前Context是否是Activity类型（包含间接包装的情况）
 */
val Context.isActivity: Boolean
    get() {
        var current: Context? = this
        while (current != null) {
            when (current) {
                is Activity -> return true
                is ContextWrapper -> current = current.baseContext
                else -> return false
            }
        }
        return false
    }


/**
 * 获取应用启动页的 [Intent]，即在 AndroidManifest.xml 中配置了 action = MAIN、category = LAUNCHER 的入口 Activity。
 *
 * @return 启动页的 [Intent]，如果获取不到则返回 null
 */
fun Context.getLauncherActivityIntent(): Intent? {
    return packageManager.getLaunchIntentForPackage(packageName)
}

inline fun <reified A : Activity> Context.startActivity(configIntent: Intent.() -> Unit = {}) {
    val intent = Intent(this, A::class.java)
    if (this !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.apply(configIntent)
    startActivity(intent)
}

inline fun <reified T : Service> Context.startService(configIntent: Intent.() -> Unit = {}) {
    startService(Intent(this, T::class.java).apply(configIntent))
}

@SuppressLint("ImplicitSamInstance")
inline fun <reified T : Service> Context.stopService() {
    stopService(Intent(this, T::class.java))
}

@SuppressLint("UnspecifiedImmutableFlag")
inline fun <reified T : Service> Context.servicePendingIntent(
    action: String,
    requestCode: Int = 0,
    configIntent: Intent.() -> Unit = {}
): PendingIntent? {
    val intent = Intent(this, T::class.java)
    intent.action = action
    configIntent.invoke(intent)
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        FLAG_UPDATE_CURRENT or FLAG_MUTABLE
    } else {
        FLAG_UPDATE_CURRENT
    }
    return getService(this, requestCode, intent, flags)
}

@SuppressLint("UnspecifiedImmutableFlag")
fun Context.activityPendingIntent(
    intent: Intent,
    action: String
): PendingIntent? {
    intent.action = action
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        FLAG_UPDATE_CURRENT or FLAG_MUTABLE
    } else {
        FLAG_UPDATE_CURRENT
    }
    return getActivity(this, 0, intent, flags)
}

@SuppressLint("UnspecifiedImmutableFlag")
inline fun <reified T : Activity> Context.activityPendingIntent(
    action: String,
    configIntent: Intent.() -> Unit = {}
): PendingIntent? {
    val intent = Intent(this, T::class.java)
    intent.action = action
    configIntent.invoke(intent)
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        FLAG_UPDATE_CURRENT or FLAG_MUTABLE
    } else {
        FLAG_UPDATE_CURRENT
    }
    return getActivity(this, 0, intent, flags)
}

@SuppressLint("UnspecifiedImmutableFlag")
inline fun <reified T : BroadcastReceiver> Context.broadcastPendingIntent(
    action: String,
    configIntent: Intent.() -> Unit = {}
): PendingIntent? {
    val intent = Intent(this, T::class.java)
    intent.action = action
    configIntent.invoke(intent)
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        FLAG_UPDATE_CURRENT or FLAG_MUTABLE
    } else {
        FLAG_UPDATE_CURRENT
    }
    return getBroadcast(this, 0, intent, flags)
}

/**
 * 注册广播接收器并自动绑定生命周期
 *
 * @param context 用于注册接收器的上下文（建议使用 ApplicationContext 避免内存泄漏）
 * @param actions 要监听的广播 Action 数组
 * @param onReceive 广播接收回调函数
 */
inline fun LifecycleOwner.registerBroadcastReceiver(
    context: Context,
    vararg actions: String,
    crossinline onReceive: (intent: Intent) -> Unit
): BroadcastReceiver {
    // 创建广播接收器实例
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            onReceive(intent)
        }
    }

    // 创建 IntentFilter 并添加 Action
    val filter = IntentFilter().apply {
        actions.forEach { addAction(it) }
    }

    // 注册广播接收器
    context.registerReceiver(receiver, filter)

    // 绑定生命周期管理
    lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_DESTROY) {
                context.unregisterReceiver(receiver)
                lifecycle.removeObserver(this)
            }
        }
    })

    return receiver
}

@SuppressLint("UnspecifiedRegisterReceiverFlag")
inline fun LifecycleOwner.registerBroadcastReceiver(
    context: Context,
    intentFilter: IntentFilter.() -> Unit,
    crossinline onReceive: (intent: Intent) -> Unit
): BroadcastReceiver {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            onReceive(intent)
        }
    }
    val filter = IntentFilter().apply(intentFilter)
    context.registerReceiver(receiver, filter)

    lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_DESTROY) {
                context.unregisterReceiver(receiver)
                lifecycle.removeObserver(this)
            }
        }
    })

    return receiver
}

fun Context.startForegroundServiceCompat(intent: Intent) {
    try {
        startService(intent)
    } catch (e: IllegalStateException) {
        ContextCompat.startForegroundService(this, intent)
    }
}


val Context.defaultSharedPreferences: SharedPreferences get() = Preferences.sp

fun Context.getPrefBoolean(key: String, defValue: Boolean = false) =
    Preferences.getValue(key, defValue)

fun Context.putPrefBoolean(key: String, value: Boolean = false) =
    Preferences.putValue(key, value)

fun Context.getPrefInt(key: String, defValue: Int = 0) =
    Preferences.getValue(key, defValue)

fun Context.putPrefInt(key: String, value: Int) =
    Preferences.putValue(key, value)

fun Context.getPrefLong(key: String, defValue: Long = 0L) =
    Preferences.getValue(key, defValue)

fun Context.putPrefLong(key: String, value: Long) =
    Preferences.putValue(key, value)

fun Context.getPrefString(key: String, defValue: String = "") =
    Preferences.getValue(key, defValue)

fun Context.putPrefString(key: String, value: String) =
    Preferences.putValue(key, value)

fun Context.getPrefStringSet(
    key: String,
    defValue: MutableSet<String>? = null
): MutableSet<String>? = defaultSharedPreferences.getStringSet(key, defValue)

fun Context.putPrefStringSet(key: String, value: MutableSet<String>) =
    defaultSharedPreferences.edit { putStringSet(key, value) }

fun Context.removePref(key: String) =
    defaultSharedPreferences.edit { remove(key) }

fun Context.layout2View(@LayoutRes layout: Int): View {
    return layoutInflater.inflate(layout, null)
}

// 使用扩展函数简化构建过程 (可单独定义)
fun CustomToast.Builder.quickShow() = build().show()

/**
 * 显示Toast
 */
fun Context.toast(message: String) {
    takeIf { !it.isActivity }?.let {
        Toaster.show(message)
    } ?: run {
        CustomToast.Builder(this)
            .setMessage(message)
            .setDuration(1800)
            .quickShow()
    }
}

/**
 * 长显示Toast
 */
fun Context.toastLong(message: String) {
    takeIf { !it.isActivity }?.let {
        Toaster.show(message)
    } ?: run {
        CustomToast.Builder(this)
            .setMessage(message)
            .setDuration(3600)
            .quickShow()
    }
}

@ColorInt
fun Context.getPrimaryTextColor(dark: Boolean): Int {
    return if (dark) {
        getCompatColor(R.color.md_light_primary_text)
    } else {
        getCompatColor(R.color.md_dark_primary_text)
    }
}

fun Context.getCompatColor(@ColorRes id: Int): Int = ContextCompat.getColor(this, id)

fun Context.getCompatDrawable(@DrawableRes id: Int): Drawable? = ContextCompat.getDrawable(this, id)

fun Context.getCompatColorStateList(@ColorRes id: Int): ColorStateList? =
    ContextCompat.getColorStateList(this, id)

fun Context.checkSelfUriPermission(uri: Uri, modeFlags: Int): Int =
    checkUriPermission(uri, Process.myPid(), Process.myUid(), modeFlags)

fun Context.restart() {
    val intent: Intent? = packageManager.getLaunchIntentForPackage(packageName)
    intent?.let {
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
                    or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    or Intent.FLAG_ACTIVITY_CLEAR_TOP
        )
        startActivity(intent)
        //杀掉以前进程
        Process.killProcess(Process.myPid())
        exitProcess(0)
    }
}

/**
 * 系统息屏时间
 */
val Context.sysScreenOffTime: Int
    get() {
        return kotlin.runCatching {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
        }.onFailure {
            it.printOnDebug()
        }.getOrDefault(0)
    }

val Context.statusBarHeight: Int
    @SuppressLint("DiscouragedApi", "InternalInsetResource")
    get() {
        if (Build.BOARD == "windows") {
            return 0
        }
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return resources.getDimensionPixelSize(resourceId)
    }

val Context.navigationBarHeight: Int
    @SuppressLint("DiscouragedApi", "InternalInsetResource")
    get() {
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return resources.getDimensionPixelSize(resourceId)
    }

/**
 * 获取电量
 */
val Context.sysBattery: Int
    get() {
        val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = registerReceiver(null, iFilter)
        return batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    }

val Context.screenRealWidthPx: Int
    get() {
        val point = Point()
        windowManager.defaultDisplay.getRealSize(point)
        return point.x
    }

val Context.screenWidthPx: Int
    get() {
        val point = Point()
        windowManager.defaultDisplay.getSize(point)
        return point.x
    }

val Context.screenRealHeightPx: Int
    get() {
        val point = Point()
        windowManager.defaultDisplay.getRealSize(point)
        return point.y
    }


val Context.screenHeightPx: Int
    get() {
        val point = Point()
        windowManager.defaultDisplay.getSize(point)
        return point.y
    }

val Context.screenRealWidthDp: Int
    get() {
        return screenRealWidthPx.pxToDp()
    }

val Context.screenRealHeightDp: Int
    get() {
        return screenRealHeightPx.pxToDp()
    }

val Context.screenWidthDp: Int
    get() {
        return screenWidthPx.pxToDp()
    }

val Context.screenHeightDp: Int
    get() {
        return screenHeightPx.pxToDp()
    }

val Context.isPad: Boolean
    get() {
        return (resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }

val Context.channel: String
    get() {
        try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            return appInfo.metaData.getString("channel") ?: ""
        } catch (e: Exception) {
            e.printOnDebug()
        }
        return ""
    }

// **Context.isDebuggable：关注系统层面是否允许调试，适合安全校验。
// **BuildConfig.DEBUG：关注构建类型，适合功能开关。
val Context.isDebuggable: Boolean
    get() = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

val Context.isSystemApp: Boolean
    get() = applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0

val Context.appPackageName: String
    get() = packageName

val Context.appName: String
    get() {
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            return appInfo.loadLabel(packageManager).toString()
        } catch (e: Exception) {
            e.logPrint()
        }
        return ""
    }

val Context.appVersionName: String
    get() {
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            return pInfo.versionName ?: ""
        } catch (e: Exception) {
            e.logPrint()
        }
        return ""
    }

val Context.appVersionCode: Int
    get() {
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            return pInfo.versionCode
        } catch (e: Exception) {
            e.logPrint()
        }
        return 0
    }