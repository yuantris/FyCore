package io.core.common.base.component.service

import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.CallSuper
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import io.core.R
import io.core.common.helper.LifecycleHelp
import io.core.common.helper.coroutine.Coroutine
import io.core.common.util.ext.ui.getActivity
import io.core.common.util.ext.ui.startService
import io.core.common.util.log.LogUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import java.security.Permissions
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
        if (isForegroundService()) checkPermission()
    }

    @CallSuper
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LogUtils.logD(message = "onStartCommand $intent ${intent?.toUri(0)}")
        if (!isForeground && isForegroundService()) {
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
     * 是否是前台服务
     */
    abstract fun isForegroundService(): Boolean

    /**
     * 开启前台服务并发送通知
     */
    open fun startForegroundNotification() {

    }

    /**
     * 检测通知权限和后台权限
     */
    private fun checkPermission() {
        LifecycleHelp.getTopActivity()?.let {
            XXPermissions.with(it)
                .permission(Permission.POST_NOTIFICATIONS)
                .request { _, _ ->
                    if (lifecycleScope.isActive) {
                        startForegroundNotification()
                    }
                }
        }
    }
}
