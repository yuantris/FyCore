package io.core.engine.storage

import android.content.Context
import com.tencent.mmkv.MMKV

// 配置类
data class StorageConfig @JvmOverloads constructor(
    val type: StorageType = StorageType.SHARED_PREFS,
    val name: String = "fycore_storage",
    val mode: Int = Context.MODE_PRIVATE,
    val mmkvMode: Int = MMKV.SINGLE_PROCESS_MODE,
    val validateClass: Class<*>? = null
)