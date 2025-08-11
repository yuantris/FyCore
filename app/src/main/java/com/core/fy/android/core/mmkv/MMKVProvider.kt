package com.core.fy.android.core.mmkv

import com.tencent.mmkv.MMKV

interface MMKVProvider {
    val mmkv: MMKV
}

// 默认全局单例，也可以按需换成不同 mmapID、多进程模式
val defaultMMKV: MMKV = MMKV.defaultMMKV()

// 内联工厂函数
inline fun <reified T> MMKVProvider.mmkv(
    key: String? = null,
    default: T
) = MMKVDelegate(key, default)