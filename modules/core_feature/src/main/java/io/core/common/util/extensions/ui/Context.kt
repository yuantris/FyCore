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
import android.content.ServiceConnection
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
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.View
import android.widget.Toast
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
import io.core.common.base.component.custom.ToastGT
import io.core.common.util.Preferences
import io.core.common.util.Toaster
import io.core.common.util.extensions.cool.logPrint
import io.core.common.util.extensions.cool.printOnDebug
import io.core.common.util.extensions.cool.pxToDp
import io.core.common.util.extensions.layoutInflater
import io.core.common.util.extensions.windowManager
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

inline fun <reified A : Activity> Context.startActivity(
    options: Bundle? = null,
    configIntent: Intent.() -> Unit = {}
) {
    val intent = Intent(this, A::class.java)
    if (this !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.apply(configIntent)
    startActivity(intent, options)
}

inline fun <reified T : Service> Context.startService(
    useForegroundService: Boolean = false,
    configIntent: Intent.() -> Unit = {}
) {
    val intent = Intent(this, T::class.java).apply(configIntent)
    if (useForegroundService && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ContextCompat.startForegroundService(this, intent)
    } else {
        startService(intent)
    }
}

fun Context.startForegroundServiceCompat(intent: Intent) {
    try {
        startService(intent)
    } catch (e: IllegalStateException) {
        ContextCompat.startForegroundService(this, intent)
    }
}

inline fun <reified T : Service> Context.bindService(
    connection: ServiceConnection,
    flags: Int = Context.BIND_AUTO_CREATE,
    configIntent: Intent.() -> Unit = {}
): Boolean {
    val intent = Intent(this, T::class.java).apply(configIntent)
    return bindService(intent, connection, flags)
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

/**
 * 注册广播接收器并自动绑定生命周期（IntentFilter版本）
 * 支持更复杂的IntentFilter配置
 *
 * @param context 用于注册接收器的上下文
 * @param intentFilter IntentFilter配置回调
 * @param onReceive 广播接收回调函数
 * @return 创建的BroadcastReceiver实例
 */
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

// ========================================
// 5. 偏好设置操作扩展
// ========================================

/**
 * 获取默认的SharedPreferences实例
 * 基于全局Preferences工具类
 */
val Context.defaultSharedPreferences: SharedPreferences get() = Preferences.sp

/**
 * 获取Boolean类型偏好设置值
 *
 * @param key 键名
 * @param defValue 默认值，默认为false
 * @return Boolean值
 */
fun Context.getPrefBoolean(key: String, defValue: Boolean = false) =
    Preferences.getValue(key, defValue)

/**
 * 设置Boolean类型偏好设置值
 *
 * @param key 键名
 * @param value 要设置的值，默认为false
 */
fun Context.putPrefBoolean(key: String, value: Boolean = false) =
    Preferences.putValue(key, value)

/**
 * 获取Int类型偏好设置值
 *
 * @param key 键名
 * @param defValue 默认值，默认为0
 * @return Int值
 */
fun Context.getPrefInt(key: String, defValue: Int = 0) =
    Preferences.getValue(key, defValue)

/**
 * 设置Int类型偏好设置值
 *
 * @param key 键名
 * @param value 要设置的值
 */
fun Context.putPrefInt(key: String, value: Int) =
    Preferences.putValue(key, value)

/**
 * 获取Long类型偏好设置值
 *
 * @param key 键名
 * @param defValue 默认值，默认为0L
 * @return Long值
 */
fun Context.getPrefLong(key: String, defValue: Long = 0L) =
    Preferences.getValue(key, defValue)

/**
 * 设置Long类型偏好设置值
 *
 * @param key 键名
 * @param value 要设置的值
 */
fun Context.putPrefLong(key: String, value: Long) =
    Preferences.putValue(key, value)

/**
 * 获取String类型偏好设置值
 *
 * @param key 键名
 * @param defValue 默认值，默认为空字符串
 * @return String值
 */
fun Context.getPrefString(key: String, defValue: String = "") =
    Preferences.getValue(key, defValue)

/**
 * 设置String类型偏好设置值
 *
 * @param key 键名
 * @param value 要设置的值
 */
fun Context.putPrefString(key: String, value: String) =
    Preferences.putValue(key, value)

/**
 * 获取StringSet类型偏好设置值
 *
 * @param key 键名
 * @param defValue 默认值，默认为null
 * @return MutableSet<String>?
 */
fun Context.getPrefStringSet(
    key: String,
    defValue: MutableSet<String>? = null
): MutableSet<String>? = defaultSharedPreferences.getStringSet(key, defValue)

/**
 * 设置StringSet类型偏好设置值
 *
 * @param key 键名
 * @param value 要设置的值
 */
fun Context.putPrefStringSet(key: String, value: MutableSet<String>) =
    defaultSharedPreferences.edit { putStringSet(key, value) }

/**
 * 移除指定键的偏好设置
 *
 * @param key 要移除的键名
 */
fun Context.removePref(key: String) =
    defaultSharedPreferences.edit { remove(key) }

// ========================================
// 6. 资源获取与UI操作扩展
// ========================================

/**
 * 将布局资源转换为View对象
 *
 * @param layout 布局资源ID
 * @return 创建的View对象
 */
fun Context.layout2View(@LayoutRes layout: Int): View {
    return layoutInflater.inflate(layout, null)
}

/**
 * 显示Toast消息
 * 根据Context类型自动选择合适的Toast显示方式
 *
 * @param message 要显示的消息
 */
fun Context.toast(message: String?) {
    takeIf { !it.isActivity }?.let {
        Toaster.show(message)
    } ?: run {
        ToastGT.show(this, message ?: "")
    }
}

/**
 * 显示长时间Toast消息
 * 根据Context类型自动选择合适的Toast显示方式
 *
 * @param message 要显示的消息
 */
fun Context.toastLong(message: String?) {
    takeIf { !it.isActivity }?.let {
        Toaster.show(message, Toast.LENGTH_LONG)
    } ?: run {
        ToastGT.show(this, message ?: "", duration = 4000)
    }
}

/**
 * 获取主要文本颜色
 * 根据主题模式返回相应的文本颜色
 *
 * @param dark 是否为深色主题
 * @return 文本颜色值
 */
@ColorInt
fun Context.getPrimaryTextColor(dark: Boolean): Int {
    return if (dark) {
        getCompatColor(R.color.md_light_primary_text)
    } else {
        getCompatColor(R.color.md_dark_primary_text)
    }
}

/**
 * 获取兼容的颜色资源
 *
 * @param id 颜色资源ID
 * @return 颜色值
 */
fun Context.getCompatColor(@ColorRes id: Int): Int = ContextCompat.getColor(this, id)

/**
 * 获取兼容的Drawable资源
 *
 * @param id Drawable资源ID
 * @return Drawable对象，获取失败返回null
 */
fun Context.getCompatDrawable(@DrawableRes id: Int): Drawable? = ContextCompat.getDrawable(this, id)

/**
 * 获取兼容的ColorStateList资源
 *
 * @param id ColorStateList资源ID
 * @return ColorStateList对象，获取失败返回null
 */
fun Context.getCompatColorStateList(@ColorRes id: Int): ColorStateList? =
    ContextCompat.getColorStateList(this, id)

// ========================================
// 7. 系统操作扩展
// ========================================

/**
 * 检查自身对URI的权限
 *
 * @param uri 要检查的URI
 * @param modeFlags 权限模式标志
 * @return 权限检查结果
 */
fun Context.checkSelfUriPermission(uri: Uri, modeFlags: Int): Int =
    checkUriPermission(uri, Process.myPid(), Process.myUid(), modeFlags)

/**
 * 重启应用
 * 清除任务栈并重新启动应用的启动页
 */
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

// ========================================
// 8. 系统信息获取扩展
// ========================================

/**
 * 获取系统息屏时间（毫秒）
 * 从系统设置中获取屏幕自动关闭的时间
 *
 * @return 息屏时间（毫秒），获取失败返回0
 */
val Context.sysScreenOffTime: Int
    get() {
        return kotlin.runCatching {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
        }.onFailure {
            it.printOnDebug()
        }.getOrDefault(0)
    }

/**
 * 获取状态栏高度（像素）
 * 通过系统资源获取状态栏的高度
 *
 * @return 状态栏高度（px），Windows平台返回0
 */
val Context.statusBarHeight: Int
    @SuppressLint("DiscouragedApi", "InternalInsetResource")
    get() {
        if (Build.BOARD == "windows") {
            return 0
        }
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return resources.getDimensionPixelSize(resourceId)
    }

/**
 * 获取导航栏高度（像素）
 * 通过系统资源获取导航栏的高度
 *
 * @return 导航栏高度（px）
 */
val Context.navigationBarHeight: Int
    @SuppressLint("DiscouragedApi", "InternalInsetResource")
    get() {
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return resources.getDimensionPixelSize(resourceId)
    }

/**
 * 获取当前电池电量百分比
 * 通过广播接收器获取电池状态信息
 *
 * @return 电量百分比（0-100），获取失败返回-1
 */
val Context.sysBattery: Int
    get() {
        val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = registerReceiver(null, iFilter)
        return batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    }

// ========================================
// 9. 屏幕尺寸信息扩展
// ========================================

/**
 * 获取屏幕真实宽度（像素）
 * 包含系统装饰（如导航栏）的完整屏幕宽度
 *
 * @return 屏幕真实宽度（px）
 */
val Context.screenRealWidthPx: Int
    get() {
        val point = Point()
        windowManager.defaultDisplay.getRealSize(point)
        return point.x
    }

/**
 * 获取屏幕可用宽度（像素）
 * 不包含系统装饰的可用屏幕宽度
 *
 * @return 屏幕可用宽度（px）
 */
val Context.screenWidthPx: Int
    get() {
        val point = Point()
        windowManager.defaultDisplay.getSize(point)
        return point.x
    }

/**
 * 获取屏幕真实高度（像素）
 * 包含系统装饰（如状态栏、导航栏）的完整屏幕高度
 *
 * @return 屏幕真实高度（px）
 */
val Context.screenRealHeightPx: Int
    get() {
        val point = Point()
        windowManager.defaultDisplay.getRealSize(point)
        return point.y
    }

/**
 * 获取屏幕可用高度（像素）
 * 不包含系统装饰的可用屏幕高度
 *
 * @return 屏幕可用高度（px）
 */
val Context.screenHeightPx: Int
    get() {
        val point = Point()
        windowManager.defaultDisplay.getSize(point)
        return point.y
    }

/**
 * 获取屏幕真实宽度（DP）
 * 将像素值转换为密度无关像素
 *
 * @return 屏幕真实宽度（dp）
 */
val Context.screenRealWidthDp: Int
    get() {
        return screenRealWidthPx.pxToDp()
    }

/**
 * 获取屏幕真实高度（DP）
 * 将像素值转换为密度无关像素
 *
 * @return 屏幕真实高度（dp）
 */
val Context.screenRealHeightDp: Int
    get() {
        return screenRealHeightPx.pxToDp()
    }

/**
 * 获取屏幕可用宽度（DP）
 * 将像素值转换为密度无关像素
 *
 * @return 屏幕可用宽度（dp）
 */
val Context.screenWidthDp: Int
    get() {
        return screenWidthPx.pxToDp()
    }

/**
 * 获取屏幕可用高度（DP）
 * 将像素值转换为密度无关像素
 *
 * @return 屏幕可用高度（dp）
 */
val Context.screenHeightDp: Int
    get() {
        return screenHeightPx.pxToDp()
    }

/**
 * 判断当前设备是否为平板
 * 基于屏幕尺寸配置判断设备类型
 *
 * @return true表示是平板设备，false表示是手机设备
 */
val Context.isPad: Boolean
    get() {
        return (resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }

// ========================================
// 10. 应用信息获取扩展
// ========================================

/**
 * 获取应用渠道信息
 * 从AndroidManifest.xml的meta-data中读取channel值
 *
 * @return 渠道字符串，获取失败返回空字符串
 */
val Context.channel: String
    get() {
        try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            return appInfo.metaData.getString("channel").orEmpty()
        } catch (e: Exception) {
            e.printOnDebug()
        }
        return ""
    }

/**
 * 判断应用是否为可调试版本
 * 检查系统层面是否允许调试，适合安全校验
 * 注意：与BuildConfig.DEBUG不同，这个关注系统层面的调试标志
 *
 * @return true表示可调试，false表示不可调试
 */
val Context.isDebuggable: Boolean
    get() = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

/**
 * 判断应用是否为系统应用
 * 检查应用是否安装在系统分区
 *
 * @return true表示是系统应用，false表示是用户应用
 */
val Context.isSystemApp: Boolean
    get() = applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0

/**
 * 获取应用包名
 *
 * @return 应用包名字符串
 */
val Context.appPackageName: String
    get() = packageName

/**
 * 获取应用名称
 * 从PackageManager中获取应用的显示名称
 *
 * @return 应用名称，获取失败返回空字符串
 */
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

/**
 * 获取应用版本名称
 * 从PackageInfo中获取versionName
 *
 * @return 版本名称字符串，获取失败返回空字符串
 */
val Context.appVersionName: String
    get() {
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            return pInfo.versionName.orEmpty()
        } catch (e: Exception) {
            e.logPrint()
        }
        return ""
    }

/**
 * 获取应用版本号
 * 从PackageInfo中获取versionCode
 *
 * @return 版本号整数，获取失败返回0
 */
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
