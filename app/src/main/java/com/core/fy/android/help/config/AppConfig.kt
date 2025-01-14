package com.core.fy.android.help.config

import android.content.res.Configuration
import android.content.res.Resources
import com.core.fy.android.BuildConfig
import com.core.fy.android.constants.PreferKey
import com.core.fy.android.constants.PreferKey.themeMode
import com.core.libraries.Android
import com.core.libraries.common.util.ext.appCtx
import com.core.libraries.common.util.ext.ui.defaultSharedPreferences
import com.core.libraries.common.util.ext.ui.getPrefBoolean
import com.core.libraries.common.util.ext.ui.getPrefInt
import com.core.libraries.common.util.ext.ui.getPrefString
import com.core.libraries.common.util.ext.ui.putPrefBoolean
import com.core.libraries.common.util.ext.ui.putPrefInt
import com.core.libraries.common.util.ext.ui.putPrefString

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/11 10:52
 * @description
 * @author Yuan
 */
object AppConfig {

    fun clearDefaultSP() {
        appCtx.defaultSharedPreferences.edit().clear().apply()
    }

    val sysConfiguration: Configuration = Resources.getSystem().configuration

    val Configuration.isNightMode: Boolean
        get() {
            val mode = uiMode and Configuration.UI_MODE_NIGHT_MASK
            return mode == Configuration.UI_MODE_NIGHT_YES
        }
    var isNightTheme: Boolean
        get() = when (themeMode) {
            "1" -> false
            "2" -> true
            "3" -> false
            else -> sysConfiguration.isNightMode
        }
        set(value) {
            if (isNightTheme != value) {
                if (value) {
                    appCtx.putPrefString(PreferKey.themeMode, "2")
                } else {
                    appCtx.putPrefString(PreferKey.themeMode, "1")
                }
            }
        }

    val textSelectAble: Boolean
        get() = appCtx.getPrefBoolean(PreferKey.textSelectAble, true)

    var optimizeRender = appCtx.getPrefBoolean(PreferKey.optimizeRender, false)
    var useAntiAlias = appCtx.getPrefBoolean(PreferKey.antiAlias)
    val doublePageHorizontal: String?
        get() = appCtx.getPrefString(PreferKey.doublePageHorizontal)

    var previewImageByClick: Boolean
        get() = appCtx.getPrefBoolean(PreferKey.previewImageByClick, false)
        set(value) {
            appCtx.putPrefBoolean(PreferKey.previewImageByClick, value)
        }

    var ttsTimer: Int
        get() = appCtx.getPrefInt(PreferKey.ttsTimer, 0)
        set(value) {
            appCtx.putPrefInt(PreferKey.ttsTimer, value)
        }

    var ttsEngine: String?
        get() = appCtx.getPrefString(PreferKey.ttsEngine)
        set(value) {
            appCtx.putPrefString(PreferKey.ttsEngine, value)
        }

    var ttsFlowSys: Boolean
        get() = appCtx.getPrefBoolean(PreferKey.ttsFollowSys, true)
        set(value) {
            appCtx.putPrefBoolean(PreferKey.ttsFollowSys, value)
        }

    val noAnimScrollPage: Boolean
        get() = appCtx.getPrefBoolean(PreferKey.noAnimScrollPage, false)

    const val defaultSpeechRate = 5

    var ttsSpeechRate: Int
        get() = appCtx.getPrefInt(PreferKey.ttsSpeechRate, defaultSpeechRate)
        set(value) {
            appCtx.putPrefInt(PreferKey.ttsSpeechRate, value)
        }

    val ignoreAudioFocus get() = appCtx.getPrefBoolean(PreferKey.ignoreAudioFocus, false)

    var pauseReadAloudWhilePhoneCalls
        get() = appCtx.getPrefBoolean(PreferKey.pauseReadAloudWhilePhoneCalls, false)
        set(value) = appCtx.putPrefBoolean(PreferKey.pauseReadAloudWhilePhoneCalls, value)

    val readAloudByMediaButton
        get() = appCtx.getPrefBoolean(PreferKey.readAloudByMediaButton, false)

    val mediaButtonOnExit get() = appCtx.getPrefBoolean("mediaButtonOnExit", true)

    var audioPlayUseWakeLock: Boolean
        get() = appCtx.getPrefBoolean(PreferKey.audioPlayWakeLock)
        set(value) {
            appCtx.putPrefBoolean(PreferKey.audioPlayWakeLock, value)
        }

    var clickActionTL = appCtx.getPrefInt(PreferKey.clickActionTL, 2)
    var clickActionTC = appCtx.getPrefInt(PreferKey.clickActionTC, 2)
    var clickActionTR = appCtx.getPrefInt(PreferKey.clickActionTR, 1)
    var clickActionML = appCtx.getPrefInt(PreferKey.clickActionML, 2)
    var clickActionMC = appCtx.getPrefInt(PreferKey.clickActionMC, 0)
    var clickActionMR = appCtx.getPrefInt(PreferKey.clickActionMR, 1)
    var clickActionBL = appCtx.getPrefInt(PreferKey.clickActionBL, 2)
    var clickActionBC = appCtx.getPrefInt(PreferKey.clickActionBC, 1)
    var clickActionBR = appCtx.getPrefInt(PreferKey.clickActionBR, 1)

    var pageTouchSlop: Int
        get() = appCtx.getPrefInt(PreferKey.pageTouchSlop, 0)
        set(value) {
            appCtx.putPrefInt(PreferKey.pageTouchSlop, value)
        }

    var bitmapCacheSize: Int
        get() = appCtx.getPrefInt(PreferKey.bitmapCacheSize, 50)
        set(value) {
            appCtx.putPrefInt(PreferKey.bitmapCacheSize, value)
        }

    var enableReview: Boolean
        get() = BuildConfig.DEBUG && appCtx.getPrefBoolean(PreferKey.enableReview, false)
        set(value) {
            appCtx.putPrefBoolean(PreferKey.enableReview, value)
        }

    var readBarStyleFollowPage: Boolean
        get() = appCtx.getPrefBoolean(PreferKey.readBarStyleFollowPage, false)
        set(value) {
            appCtx.putPrefBoolean(PreferKey.readBarStyleFollowPage, value)
        }

    var brightnessVwPos: Boolean
        get() = appCtx.getPrefBoolean(PreferKey.brightnessVwPos)
        set(value) {
            appCtx.putPrefBoolean(PreferKey.brightnessVwPos, value)
        }

    val progressBarBehavior: String?
        get() = appCtx.getPrefString(PreferKey.progressBarBehavior, "page")

    var readBrightness: Int
        get() = if (isNightTheme) {
            appCtx.getPrefInt(PreferKey.nightBrightness, 100)
        } else {
            appCtx.getPrefInt(PreferKey.brightness, 100)
        }
        set(value) {
            if (isNightTheme) {
                appCtx.putPrefInt(PreferKey.nightBrightness, value)
            } else {
                appCtx.putPrefInt(PreferKey.brightness, value)
            }
        }

    var showReadTitleBarAddition: Boolean
        get() = appCtx.getPrefBoolean(PreferKey.showReadTitleAddition, true)
        set(value) {
            appCtx.putPrefBoolean(PreferKey.showReadTitleAddition, value)
        }

    var contentSelectSpeakMod: Int
        get() = appCtx.getPrefInt(PreferKey.contentSelectSpeakMod)
        set(value) {
            appCtx.putPrefInt(PreferKey.contentSelectSpeakMod, value)
        }

}