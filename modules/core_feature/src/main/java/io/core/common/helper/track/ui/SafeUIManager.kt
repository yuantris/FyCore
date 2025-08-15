package io.core.common.helper.track.ui

import android.app.Activity
import io.core.common.helper.track.AppTrackV2
import io.core.common.util.extensions.cool.HandlerGT
import io.core.common.util.extensions.cool.isMainThread
import io.core.common.util.extensions.ui.isAlive
import io.core.common.util.log.LogPure
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentLinkedQueue

// 通用UI操作管理器
class SafeUIManager {
    private data class PendingUIOperation(
        val activityRef: WeakReference<Activity>,
        val operation: UIOperation
    )

    private val pendingOperations = ConcurrentLinkedQueue<PendingUIOperation>()

    fun executeUISafely(activity: Activity, operation: UIOperation) {
        if (operation.canExecute(activity) && canExecuteUIOperationNow(activity)) {
            // 直接执行
            executeOnMain { operation.execute(activity) }
        } else {
            // 加入队列
            pendingOperations.offer(PendingUIOperation(WeakReference(activity), operation))
        }
    }

    fun processPendingOperations(resumedActivity: Activity) {
        val toExecute = mutableListOf<UIOperation>()
        val iterator = pendingOperations.iterator()

        while (iterator.hasNext()) {
            val pending = iterator.next()
            val activity = pending.activityRef.get()
            val operation = pending.operation

            // 清理过期或无效的操作
            if (activity == null ||
                System.currentTimeMillis() - operation.timestamp > operation.timeout) {
                if (activity == null) {
                    operation.onTimeout()
                }
                iterator.remove()
                continue
            }

            // 如果是当前恢复的Activity且可以执行
            if (activity == resumedActivity && operation.canExecute(activity)) {
                toExecute.add(operation)
                iterator.remove()
            }
        }

        // 按优先级排序并执行
        toExecute.sortedByDescending { it.priority }.forEach { operation ->
            executeOnMain {
                try {
                    operation.execute(resumedActivity)
                } catch (e: Exception) {
                    LogPure.e("SafeUIManager", "Failed to execute UI operation: ${e.message}")
                }
            }
        }
    }

    fun clearPendingOperations(activity: Activity) {
        val iterator = pendingOperations.iterator()
        while (iterator.hasNext()) {
            val pending = iterator.next()
            if (pending.activityRef.get() == activity) {
                iterator.remove()
            }
        }
    }

    private fun canExecuteUIOperationNow(activity: Activity): Boolean {
        return activity.isAlive() &&
                !activity.isFinishing &&
                !activity.isDestroyed &&
                AppTrackV2.getTopActivity() == activity
    }

    private inline fun executeOnMain(crossinline action: () -> Unit) {
        if (isMainThread()) action() else HandlerGT.main.post { action.invoke() }
    }
}
