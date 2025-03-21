package com.core.fy.android.util

/**
 * 处理双击返回键退出应用的逻辑
 * @param interval 两次点击的时间间隔（默认 2000ms）
 * @param warnMsg 首次点击时显示的警告信息（默认 "再按一次退出APP"）
 */
class DoubleBackExitHandler @JvmOverloads constructor(
    private val interval: Long = 2000,
    private val warnMsg: String = "再按一次退出APP"
) {
    @Volatile
    private var lastBackPressTime: Long = 0

    /**
     * 处理返回键事件
     * @param onShowWarning 当需要显示警告时触发的回调
     * @param onTriggerExit 当满足退出条件时触发的回调
     */
    fun handleBackPress(
        onShowWarning: (message: String) -> Unit,
        onTriggerExit: () -> Unit
    ) {
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - lastBackPressTime

        when {
            elapsedTime >= interval -> {
                onShowWarning(warnMsg)
                lastBackPressTime = currentTime
            }
            else -> {
                reset()
                onTriggerExit()
            }
        }
    }

    /**
     * 重置计时器（可用于界面跳转时主动重置状态）
     */
    fun reset() {
        lastBackPressTime = 0
    }
}
