package com.core.fy.android.help.config

import io.core.appCtx
import io.core.common.util.extensions.cool.GSON
import io.core.common.util.extensions.cool.fromJsonArray
import java.io.File

object DefaultData {


    val readConfigs: List<ReadBookConfig.Config> by lazy {
        val json = String(
            appCtx.assets.open("defaultData${File.separator}${ReadBookConfig.configFileName}")
                .readBytes()
        )
        GSON.fromJsonArray<ReadBookConfig.Config>(json).getOrNull()
            ?: emptyList()
    }

}