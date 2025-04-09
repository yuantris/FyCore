package io.core.other

import android.util.Log

/**
 * 代码执行耗时测量工具
 */
object TimeMeasurer {

    // 支持不同时间单位
    enum class Precision { MILLISECONDS, NANOSECONDS }

    /**
     * 执行代码块并测量耗时（默认输出日志）
     * @param tag 日志标签
     * @param message 日志消息模板，可用 %d 占位符表示时间
     * @param block 要执行的代码块
     * @return 代码块的执行结果
     */
    inline fun <T> measureTime(
        tag: String = this.javaClass.simpleName,
        message: String = "Execution time: %dms",
        block: () -> T
    ): T {
        val startTime = System.currentTimeMillis()
        return try {
            block()
        } finally {
            val duration = System.currentTimeMillis() - startTime
            Log.d(tag, String.format(message, duration))
        }
    }

    /**
     * 执行代码块并返回耗时（不自动打印日志）
     * @param block 要执行的代码块
     * @return 耗时（毫秒）
     */
    inline fun measureTimeSilent(block: () -> Unit): Long {
        val startTime = System.currentTimeMillis()
        block()
        return System.currentTimeMillis() - startTime
    }

    /**
     * 高级版本：支持时间单位和自定义回调
     */
    inline fun <T> measureAdvanced(
        tag: String = this.javaClass.simpleName,
        precision: Precision = Precision.MILLISECONDS,
        onTimeMeasured: (Long) -> Unit = { duration ->
            val unit = if (precision == Precision.MILLISECONDS) "ms" else "ns"
            Log.d(tag, "耗时: $duration $unit")
        },
        block: () -> T
    ): T {
        val startTime = if (precision == Precision.MILLISECONDS) {
            System.currentTimeMillis()
        } else {
            System.nanoTime()
        }

        return try {
            block()
        } finally {
            val endTime = if (precision == Precision.MILLISECONDS) {
                System.currentTimeMillis()
            } else {
                System.nanoTime()
            }
            val duration = endTime - startTime
            onTimeMeasured(duration)
        }
    }
}