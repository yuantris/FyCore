package io.core.ui.base.component.service

import android.content.Intent
import android.os.IBinder
import androidx.annotation.CallSuper
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import io.core.common.helper.coroutine.Coroutine
import io.core.ui.helper.track.AppTrackV2
import io.core.utils.log.LogPure
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
        AppTrackV2.onServiceCreate(this)
    }

    @CallSuper
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LogPure.d(message = "onStartCommand $intent ${intent?.toUri(0)}")
        if (!isForeground && isForegroundService()) {
            startForegroundNotification()
            isForeground = true
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @CallSuper
    override fun onTaskRemoved(rootIntent: Intent?) {
        LogPure.d(message = "onTaskRemoved rootIntent:$rootIntent")
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
        AppTrackV2.onServiceDestroy(this)
    }

    /**
     * 是否是前台服�?
     */
    abstract fun isForegroundService(): Boolean

    /**
     * 开启前台服务并发送通知
     */
    open fun startForegroundNotification() {

    }

}
