package com.core.fy.android

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.core.fy.android.constants.AppConst.channelIdReadAloud
import com.core.fy.android.util.initDialogX
import io.core.Android
import io.core.BR
import io.core.appCtx
import io.core.common.CoreConfig
import io.core.common.helper.AppLifecycleTracker
import io.core.common.util.MediaScanner
import io.core.common.util.extensions.notificationManager
import io.core.common.util.log.LogPure
import io.core.engine.brv.utils.BRV


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
        Android.initialize(this)
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
        CoreConfig.crashAfterJumpActivity = MainActivity::class.java
        CoreConfig.alert_positive_color = getColor(R.color.md_indigo_500)
        CoreConfig.alert_negative_color = getColor(R.color.md_red_300)

        /**
         * 如果BRV使用DataBinding，需要初始化
         * 在Application中初始化, DataBinding会根据modelId自动绑定models到xml中
         */
        BRV.modelId = BR.m

        // SoftKeyboardGlobal.install(this, false)
        createNotificationChannels()

        AppLifecycleTracker.registerAppStatusListener { isForeground ->
            LogPure.v {
                "进入${if (isForeground) "前台" else "后台"}"
            }
        }

        MediaScanner.registerContentObserver()
    }

    /**
     * 创建通知ID
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

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