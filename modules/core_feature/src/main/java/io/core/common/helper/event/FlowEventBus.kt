package io.core.common.helper.event

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStateAtLeast
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

object FlowEventBus {

    //用HashMap存储SharedFlow
    private val flowEvents = ConcurrentHashMap<String, MutableSharedFlow<Event>>()

    //获取Flow，当相应Flow不存在时创建
    fun getFlow(key: String): MutableSharedFlow<Event> {
        return flowEvents[key] ?: MutableSharedFlow<Event>().also { flowEvents[key] = it }
    }

    // 发送事件
    fun post(event: Event, tag: String = event.javaClass.simpleName, delay: Long = 0) {
        MainScope().launch {
            delay(delay)
            getFlow(tag).emit(event)
        }
    }

    // 订阅事件
    inline fun <reified T : Event> observe(
        lifecycleOwner: LifecycleOwner,
        tag: String = T::class.java.simpleName,
        minState: Lifecycle.State = Lifecycle.State.CREATED,
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
        crossinline onReceived: (T) -> Unit
    ) = lifecycleOwner.lifecycleScope.launch(dispatcher) {
        getFlow(key = tag).collectLatest {
            lifecycleOwner.lifecycle.whenStateAtLeast(minState) {
                if (it is T) onReceived(it)
            }
        }
    }

}
