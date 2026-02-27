package io.core.utils

import android.widget.Toast
import io.core.base.appCtx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

object Toaster : CoroutineScope by MainScope() {

    private var currentToast: Toast? = null
    private var lastShowTime: Long = 0
    private const val DEBOUNCE_TIME = 1000L // 防抖时间1�?

    /**
     * 显示Toast，保证同一时间只能弹一�?
     *
     * @param message 显示的消�?
     * @param duration Toast显示时长，默认为Toast.LENGTH_SHORT
     */
    @JvmStatic
    @JvmOverloads
    fun show(message: String?, duration: Int = Toast.LENGTH_SHORT) {
        launch(Dispatchers.Main) {
            // 如果有正在显示的Toast，取消它
            currentToast?.cancel()

            // 创建新的Toast实例
            currentToast = Toast.makeText(appCtx, message, duration).apply {
                show()
            }
        }
    }

    /**
     * 显示Toast（带防抖功能�?
     * @param message 显示的消�?
     * @param duration Toast显示时长，默认为Toast.LENGTH_SHORT
     * @return Boolean 是否实际显示了Toast（防抖期内返回false�?
     */
    @JvmStatic
    @JvmOverloads
    fun showDebounced(message: String?, duration: Int = Toast.LENGTH_SHORT): Boolean {
        val currentTime = System.currentTimeMillis()
        return if (currentTime - lastShowTime > DEBOUNCE_TIME) {
            lastShowTime = currentTime
            show(message, duration)
            true
        } else {
            false
        }
    }

    /**
     * 取消当前显示的Toast
     */
    @JvmStatic
    fun cancel() {
        launch(Dispatchers.Main) {
            currentToast?.cancel()
            currentToast = null
        }
    }
}
