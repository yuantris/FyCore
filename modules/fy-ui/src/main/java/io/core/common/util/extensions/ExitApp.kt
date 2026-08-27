package io.core.common.util.extensions

import io.core.Android
import io.core.common.util.extensions.cool.runDelayedMain
import kotlin.system.exitProcess

// 结束全部Activity由具备栈管理能力的模块(track)通过 Android.registerFinishAllActivitiesHandler 注册；
// 未注册时直接退出进程，行为兜底一致。
fun Any?.exitApp() {
    Android.finishAllActivities()
    runDelayedMain(10) {
        exitProcess(0)
    }
}
