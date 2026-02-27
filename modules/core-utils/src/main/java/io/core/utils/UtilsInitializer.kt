package io.core.utils

import android.app.Application
import io.core.base.DataClearable
import io.core.base.Initializer
import io.core.utils.log.bury.AppLog

/**
 * core-utils 模块初始化器
 * 负责初始化日志系统等工具组件
 */
object UtilsInitializer : Initializer, DataClearable {
    
    override fun onInit(application: Application, debug: Boolean) {
        AppLog.initialize()
    }

    override fun onClearData() {
        Preferences.clear()
    }
}
