package io.core.common.helper.coroutine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * 在任意上下文中调用挂起函数的工具方法
 *
 * @param block 挂起函数的代码块
 */
fun launchSuspend(block: suspend CoroutineScope.() -> Unit) {
    CoroutineScope(Dispatchers.Default).launch {
        block()
    }
}

/**
 * 如果当前环境无法启动协程（比如某些测试环境），可以使用 runBlocking 包装挂起函数
 */
fun <T> runSuspend(block: suspend CoroutineScope.() -> T): T = runBlocking {
    block()
}
