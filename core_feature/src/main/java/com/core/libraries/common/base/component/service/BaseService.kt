package com.core.libraries.common.base.component.service

import android.content.Intent
import android.os.IBinder
import androidx.annotation.CallSuper
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.core.libraries.common.helper.LifecycleHelp
import com.core.libraries.common.helper.coroutine.Coroutine
import com.core.libraries.common.util.log.LogUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

abstract class BaseService : LifecycleService() {

    private val simpleName = this::class.simpleName.toString()
    private var isForeground = false

    fun <T> execute(
        scope: CoroutineScope = lifecycleScope,
        context: CoroutineContext = Dispatchers.IO,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        executeContext: CoroutineContext = Dispatchers.Main,
        block: suspend CoroutineScope.() -> T
    ) = Coroutine.async(scope, context, start, executeContext, block)

    @CallSuper
    override fun onCreate() {
        super.onCreate()
        LifecycleHelp.onServiceCreate(this)
        checkPermission()
    }

    @CallSuper
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LogUtils.logD(message = "onStartCommand $intent ${intent?.toUri(0)}")
        if (!isForeground) {
            startForegroundNotification()
            isForeground = true
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @CallSuper
    override fun onTaskRemoved(rootIntent: Intent?) {
        LogUtils.logD(message = "onTaskRemoved rootIntent:$rootIntent")
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        LifecycleHelp.onServiceDestroy(this)
    }

    /**
     * 开启前台服务并发送通知
     */
    open fun startForegroundNotification() {

    }

    /**
     * 检测通知权限和后台权限
     */
    private fun checkPermission() {
//        PermissionsCompat.Builder()
//            .addPermissions(Permissions.POST_NOTIFICATIONS)
//            .rationale(R.string.notification_permission_rationale)
//            .onGranted {
//                if (lifecycleScope.isActive) {
//                    startForegroundNotification()
//                }
//            }
//            .request()
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            PermissionsCompat.Builder()
//                .addPermissions(Permissions.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
//                .rationale(R.string.ignore_battery_permission_rationale)
//                .request()
//        }
    }
}
