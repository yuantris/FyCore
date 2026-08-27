package io.core.other

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import io.core.Android
import io.core.common.helper.track.AppTrackV2
import io.core.common.util.Preferences
import io.core.engine.livebus.LiveEventBus
import io.core.engine.livebus.logger.DefaultLogger
import io.core.engine.storage.StorageFactory
import io.core.engine.storage.storage
import io.core.nav.NavigationManager

/**
 * 迁移期过渡安装器：在 fy-core umbrella 建成前，由 core_feature 承担聚合层职责。
 * 通过 Provider 时机注册各模块初始化钩子，业务方仍调用 Android.initialize(...) 触发，行为与 1.x 完全一致。
 * M3 阶段此逻辑将迁入 fy-core 的 FyCoreInstaller，本类随之删除。
 */
class FyLegacyInstaller : ContentProvider() {

    override fun onCreate(): Boolean {
        Android.registerInitHook { app, debug -> installTrack(app) }
        Android.registerInitHook { app, debug -> installCrash(app) }
        Android.registerInitHook { _, _ -> installNav() }
        Android.registerInitHook { _, debug -> installEventBus(debug) }
        Android.registerTopActivityProvider { AppTrackV2.getTopActivity() }
        Android.registerFinishAllActivitiesHandler { AppTrackV2.finishAllActivities() }
        Android.registerClearHook {
            IntentData.clear()
            Preferences.clear()
            AppLauncher.clearCache()
            if (StorageFactory.isInit()) storage.clear()
        }
        return true
    }

    private fun installTrack(app: Application) {
        AppTrackV2.init(app)
    }

    private fun installCrash(app: Application) {
        CrashHandler.register(app)
    }

    private fun installNav() {
        NavigationManager.initialize()
    }

    private fun installEventBus(debug: Boolean) {
        LiveEventBus.config().apply {
            lifecycleObserverAlwaysActive(true)
            autoClear(true)
            enableLogger(debug)
            setLogger(DefaultLogger())
        }
    }

    // ---- ContentProvider 模板方法（不使用）----
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
