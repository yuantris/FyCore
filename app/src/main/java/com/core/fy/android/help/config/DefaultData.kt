package com.core.fy.android.help.config

import com.core.libraries.common.util.ext.appCtx
import com.core.libraries.common.util.ext.cool.GSON
import com.core.libraries.common.util.ext.cool.fromJsonArray
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