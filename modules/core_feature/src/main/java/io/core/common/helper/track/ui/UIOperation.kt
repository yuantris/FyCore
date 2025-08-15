package io.core.common.helper.track.ui

import android.app.Activity
import android.app.Dialog
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import io.core.common.util.extensions.ui.isAlive
import io.core.common.util.log.LogPure

// UI操作接口
interface UIOperation {
    val priority: Int get() = 0
    val timeout: Long get() = 30_000L
    val timestamp: Long get() = System.currentTimeMillis()

    fun execute(activity: Activity): Boolean
    fun canExecute(activity: Activity): Boolean
    fun onTimeout()
}

// 具体UI操作实现
class ToastOperation(
    private val message: String,
    private val duration: Int = Toast.LENGTH_SHORT
) : UIOperation {
    override fun execute(activity: Activity): Boolean {
        return try {
            Toast.makeText(activity, message, duration).show()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun canExecute(activity: Activity): Boolean {
        return activity.isAlive() && !activity.isFinishing && !activity.isDestroyed
    }

    override fun onTimeout() {
        LogPure.w("UIOperation", "Toast operation timeout: $message")
    }
}

class DialogOperation(
    private val dialogBuilder: (Activity) -> Dialog
) : UIOperation {
    override val priority: Int = 1 // 更高优先级

    override fun execute(activity: Activity): Boolean {
        return try {
            val dialog = dialogBuilder(activity)
            if (activity.isAlive() && !activity.isFinishing) {
                dialog.show()
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    override fun canExecute(activity: Activity): Boolean {
        return activity.isAlive() && !activity.isFinishing && !activity.isDestroyed
    }

    override fun onTimeout() {
        LogPure.w("UIOperation", "Dialog operation timeout")
    }
}

class SnackbarOperation(
    private val view: View,
    private val message: String,
    private val duration: Int = Snackbar.LENGTH_SHORT
) : UIOperation {
    override fun execute(activity: Activity): Boolean {
        return try {
            if (view.isAttachedToWindow) {
                Snackbar.make(view, message, duration).show()
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    override fun canExecute(activity: Activity): Boolean {
        return activity.isAlive() && view.isAttachedToWindow
    }

    override fun onTimeout() {
        LogPure.w("UIOperation", "Snackbar operation timeout: $message")
    }
}
