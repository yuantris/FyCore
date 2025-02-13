package io.core.common.util

import android.widget.Toast
import io.core.common.util.ext.appCtx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

object ToastUtil : CoroutineScope by MainScope() {

    private var currentToast: Toast? = null

    /**
     * 显示Toast，保证同一时间只能弹一次
     *
     * @param message 显示的消息
     * @param duration Toast显示时长，默认为Toast.LENGTH_SHORT
     */
    fun show(message: String, duration: Int = Toast.LENGTH_SHORT) {
        launch(Dispatchers.Main) {
            // 如果有正在显示的Toast，取消它
            currentToast?.cancel()

            // 创建新的Toast实例
            currentToast = Toast.makeText(appCtx, message, duration).apply {
                show()
            }
        }
    }

    @JvmStatic
    fun showShort(message: String) {
        show(message, Toast.LENGTH_SHORT)
    }

    @JvmStatic
    fun showLong(message: String) {
        show(message, Toast.LENGTH_LONG)
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
