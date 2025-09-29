package com.core.fy.android

import android.app.Activity
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import com.core.fy.android.constants.AppConst.channelIdReadAloud
import com.core.fy.android.constants.PreferKey
import com.core.fy.android.function.fold.FoldActivity
import com.core.fy.android.function.tts.ClickTextActivity
import com.core.fy.android.util.HttpClient
import com.core.fy.android.util.NetworkException
import com.core.fy.android.util.initDialogX
import com.tencent.mmkv.MMKV
import io.core.Android
import io.core.BR
import io.core.common.CoreConfig
import io.core.common.helper.coroutine.info.GlobalCoroutine
import io.core.common.helper.net.NetworkMonitor
import io.core.common.helper.track.AppTrackV2
import io.core.common.helper.track.TurboTracker
import io.core.common.helper.track.v3.AppTrackV3
import io.core.common.helper.track.v3.AppTrackV3Helper
import io.core.common.util.CoreUtil.Companion.toast
import io.core.common.util.extensions.notificationManager
import io.core.common.util.log.LogPure
import io.core.common.util.tools.OSAir
import io.core.constant.ANDROID_8
import io.core.engine.brv.utils.BRV
import io.core.engine.storage.StorageFactory
import io.core.engine.storage.StorageType
import io.core.nav.AppAuthProvider
import io.core.nav.AuthProvider
import io.core.nav.LoginActivity
import io.core.nav.LoginInterceptor
import io.core.nav.NavigationInterceptor
import io.core.nav.NavigationListener
import io.core.nav.NavigationManager
import io.core.nav.NavigationRequest
import io.core.nav.OrderActivity
import io.core.nav.ProfileActivity
import io.core.nav.SettingsActivity
import io.core.other.CrashHandler


/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2024/12/24 8:54
 * @description
 * @author Yuan
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Android.initialize(this, debug = true, "1234")
        TurboTracker.initialize(this) {
            enable(CoreConfig.Environment.isDebug)
            brief(true)
        }
        CrashHandler.startPeriodicTask()
        StorageFactory.initialize {
            type = StorageType.MMKV
            mmkvMode = MMKV.MULTI_PROCESS_MODE
            validateClass = PreferKey::class.java
        }

        initDialogX()

//        // 添加日志全局拦截器
//        LogCat.addHook(object : LogHook {
//            override fun hook(info: LogInfo) {
//                GSON.toJson(info).logV("LogHook_")
//            }
//        })
        initApp()
    }

    private fun initApp() {

        /**
         * 如果BRV使用DataBinding，需要初始化
         * 在Application中初始化, DataBinding会根据modelId自动绑定models到xml中
         */
        BRV.modelId = BR.m

        // SoftKeyboardGlobal.install(this, false)
        createNotificationChannels()

        NetworkMonitor.initialize()
        AppTrackV2.registerAppStatusListener { isForeground ->
            LogPure.v {
                "进入${if (isForeground) "前台" else "后台"}"
            }
        }

        // 初始化导航
        NavigationManager.configure {
            setListener(object : NavigationListener{

                override fun onNavigationStart(request: NavigationRequest) {
                    super.onNavigationStart(request)
                    LogPure.v {
                        "开始导航: ${request.targetClass<Activity>()?.simpleName}"
                    }
                }
                override fun onInterceptorExecute(
                    interceptor: NavigationInterceptor,
                    request: NavigationRequest
                ) {
                    super.onInterceptorExecute(interceptor, request)
                    LogPure.v {
                        "拦截器: ${interceptor.name} 拦截了请求: ${request.targetClass<Activity>()?.simpleName}"
                    }
                }
            })
            // 登录拦截器
            addInterceptor(
                LoginInterceptor(
                    authProvider = object : AuthProvider{
                        override suspend fun isLoggedIn(): Boolean {
                            return false
                        }

                        override fun isLoggedInSync(): Boolean {
                            return false
                        }

                    },
                    loginActivityClass = ClickTextActivity::class,
                    protectedActivities = setOf(
                        FoldActivity::class,
                    )
                )
            )
        }

        GlobalCoroutine.launch {
            val result = HttpClient.getConfig()
            result.fold(
                onSuccess = { json ->
                    // 解析 json
                    LogPure.d { "json: $json" }
                },
                onFailure = { ex ->
                    val msg = when (ex) {
                        is NetworkException.IO -> "网络不给力，请稍后再试"
                        is NetworkException.Http -> "服务器开小差了(${ex.code})"
                        else -> "获取配置失败"
                    }
                    toast(msg)
                }
            )
        }
    }

    /**
     * 创建通知ID
     */
    private fun createNotificationChannels() {
        if (OSAir.lowerThan(ANDROID_8)) return

        val readAloudChannel = NotificationChannel(
            channelIdReadAloud,
            getString(R.string.read_aloud),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }


        //向notification manager 提交channel
        notificationManager.createNotificationChannels(
            listOf(
                readAloudChannel
            )
        )
    }
}