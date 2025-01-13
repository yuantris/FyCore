package com.core.libraries.common.base.event.channel

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.WeakHashMap
import kotlin.coroutines.CoroutineContext

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/7 8:54
 * @description
 * @author Yuan
 */

/**
 * 异步协程作用域
 */
@PublishedApi
internal open class ChannelScope() : CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main.immediate + SupervisorJob()

    // 使用 WeakHashMap 管理 Observer
    companion object {
        private val lifecycleObservers = WeakHashMap<LifecycleOwner, LifecycleEventObserver>()
    }

    constructor(
        lifecycleOwner: LifecycleOwner,
        lifeEvent: Lifecycle.Event = Lifecycle.Event.ON_DESTROY
    ) : this() {
        // 检查是否已存在 Observer
        if (!lifecycleObservers.containsKey(lifecycleOwner)) {
            val observer = object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (lifeEvent == event) {
                        cancel()
                        lifecycleOwner.lifecycle.removeObserver(this) // 手动移除
                        lifecycleObservers.remove(lifecycleOwner) // 从 WeakHashMap 中移除
                    }
                }
            }
            lifecycleObservers[lifecycleOwner] = observer
            lifecycleOwner.lifecycle.addObserver(observer)
        }
    }
}