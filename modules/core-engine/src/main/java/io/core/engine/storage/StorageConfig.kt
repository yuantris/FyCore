package io.core.engine.storage

import android.content.Context
import com.tencent.mmkv.MMKV
import io.core.constant.SP_NAME

// 配置�?
data class StorageConfig @JvmOverloads constructor(
    var type: StorageType = StorageType.SHARED_PREFS,
    var name: String = SP_NAME,
    var mode: Int = Context.MODE_PRIVATE,
    var mmkvMode: Int = MMKV.SINGLE_PROCESS_MODE,
    var validateClass: Class<*>? = null
)