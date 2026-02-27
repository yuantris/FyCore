package io.core.ui

import android.app.Application
import io.core.base.Initializer
import io.core.ui.helper.track.AppTrackV2

/**
 * core-ui 模块初始化器
 * 负责初始化UI相关组件，如Activity生命周期追踪
 */
object UiInitializer : Initializer {
    
    override fun onInit(application: Application, debug: Boolean) {
        AppTrackV2.init(application)
    }
}
