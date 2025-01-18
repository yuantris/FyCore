package io.core.common.helper.event.channel

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    // 使用 WeakHashMap 管理 Observer
    companion object {
        private val lifecycleObservers = WeakHashMap<LifecycleOwner, LifecycleEventObserver>()
    }

    private val job = SupervisorJob()

    override val coroutineContext: CoroutineContext = Dispatchers.Main.immediate + job

    constructor(
        lifecycleOwner: LifecycleOwner,
        lifeEvent: Lifecycle.Event = Lifecycle.Event.ON_DESTROY
    ) : this() {
        // 检查是否已存在 Observer
        if (!lifecycleObservers.containsKey(lifecycleOwner)) {
            val observer = object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (lifeEvent == event) {
                        if (job.isActive) {  // 确保协程处于活跃状态
                            job.cancel()
                            lifecycleOwner.lifecycle.removeObserver(this)
                            lifecycleObservers.remove(lifecycleOwner)
                        }
                    }
                }
            }
            lifecycleObservers[lifecycleOwner] = observer
            lifecycleOwner.lifecycle.addObserver(observer)
        }
    }
}