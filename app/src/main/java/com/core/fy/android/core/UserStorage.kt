package com.core.fy.android.core

import com.core.fy.android.core.mmkv.MMKVProvider
import com.core.fy.android.core.mmkv.defaultMMKV
import com.core.fy.android.core.mmkv.mmkv
import com.tencent.mmkv.MMKV

object UserStorage : MMKVProvider {
    override val mmkv: MMKV = defaultMMKV // 也可换成 MMKV.mmkvWithID("user")

    // 基本类型
    var loginToken by mmkv(default = "")
    var firstLaunch by mmkv(default = true)

    // 集合
    var historyIds by mmkv(default = setOf<String>())

    fun clear(key: String) {
        mmkv.remove(key)
    }

    fun clearAll() {
        mmkv.clearAll()
    }
}