package com.core.fy.android

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.core.fy.android.constants.AppConst.channelIdReadAloud
import io.core.Android
import io.core.common.CoreConfigs
import io.core.common.util.ext.cool.GSON
import io.core.common.util.ext.notificationManager
import io.core.common.util.log.LogCat
import io.core.common.util.log.LogHook
import io.core.common.util.log.LogInfo
import io.core.common.util.log.logV

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
        // 设置重启对象Activity
        Android.homeActivity = MainActivity::class.java

//        // 添加日志全局拦截器
//        LogCat.addHook(object : LogHook {
//            override fun hook(info: LogInfo) {
//                GSON.toJson(info).logV("LogHook_")
//            }
//        })
        initApp()
    }

    private fun initApp() {
        CoreConfigs.DIALOG_BUTTON_POSITIVE_COLOR = getColor(R.color.md_indigo_500)
        CoreConfigs.DIALOG_BUTTON_NEGATIVE_COLOR = getColor(R.color.md_red_300)
        // SoftKeyboardGlobal.install(this, false)
        createNotificationChannels()
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