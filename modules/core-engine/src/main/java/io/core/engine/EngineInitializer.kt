package io.core.engine

import android.app.Application
import io.core.base.DataClearable
import io.core.base.Initializer
import io.core.engine.livebus.LiveEventBus
import io.core.engine.livebus.logger.DefaultLogger
import io.core.engine.storage.StorageFactory
import io.core.engine.storage.storage

/**
 * core-engine 模块初始化器
 * 负责初始化引擎组件，如LiveEventBus
 */
object EngineInitializer : Initializer, DataClearable {
    
    override fun onInit(application: Application, debug: Boolean) {
        LiveEventBus.config().apply {
            lifecycleObserverAlwaysActive(true)
            autoClear(true)
            enableLogger(debug)
            setLogger(DefaultLogger())
        }
    }

    override fun onClearData() {
        if (StorageFactory.isInit()) {
            storage.clear()
        }
    }
}
