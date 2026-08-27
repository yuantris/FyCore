package io.core.common.helper

import java.util.*

/**
 * 通用状态事务管理工具（支持嵌套事务和跨方法状态管理）
 * 使用场景：
 * 1. 自动作用域事务（推荐在单个代码块内使用）
 * 2. 手动跨方法事务（适合生命周期分散的场景）
 *
 * 示例 1：自动作用域
 * StateTransactionHelper.withState(StatusBarManager.with(activity)) {
 *     updateStatusBarManually(false)
 * }
 *
 * 示例 2：手动跨方法
 * class FullscreenDialog : DialogFragment() {
 *     private var transaction: StateTransactionHelper<Color>? = null
 *
 *     override fun onStart() {
 *         super.onStart()
 *         transaction = StateTransactionHelper.beginTransaction(StatusBarManager(requireActivity()))
 *         transaction?.applyState(DARK_COLOR)
 *     }
 *
 *     override fun onStop() {
 *         super.onStop()
 *         transaction?.restore()
 *     }
 * }
 */
class StateTransactionHelper<T> private constructor(
    private val stateController: StateController<T>,
    private val stateStack: Deque<T>
) {

    // 新增状态变更LiveData
    private val _stateLiveData = androidx.lifecycle.MutableLiveData<T>()
    val stateLiveData: androidx.lifecycle.LiveData<T> = _stateLiveData

    companion object {
        private val stateStacks = WeakHashMap<StateController<*>, Deque<*>>()
        private val performanceMonitor = mutableMapOf<StateController<*>, Long>()
        // 新增事务嵌套深度限制（默认最大5层）
        @Volatile
        var MAX_DEPTH = 5
            private set

        /**
         * 动态设置最大事务嵌套深度
         * @param newDepth 新的深度限制（必须大于0）
         * @throws IllegalArgumentException 如果参数不合法
         */
        @JvmStatic
        @Synchronized
        fun setMaxDepth(newDepth: Int) {
            require(newDepth > 0) { "MAX_DEPTH must be greater than 0" }
            MAX_DEPTH = newDepth
        }

        /**
         * 创建自动作用域事务（推荐在单个代码块内使用）
         * @param stateController 状态控制器
         * @param block 事务操作块
         */
        @JvmStatic
        fun <T> withState(
            stateController: StateController<T>,
            block: (StateTransactionHelper<T>) -> Unit
        ) {
            val transaction = beginTransaction(stateController)
            try {
                block(transaction)
            } finally {
                transaction.restore()
            }
        }

        /**
         * 开始手动事务（适合跨生命周期场景）
         * @param stateController 状态控制器
         */
        @JvmStatic
        fun <T> beginTransaction(stateController: StateController<T>): StateTransactionHelper<T> {

            @Suppress("UNCHECKED_CAST")
            val stack = stateStacks.getOrPut(stateController) { ArrayDeque<T>() } as Deque<T>

            // 检查嵌套深度
            if (stack.size >= MAX_DEPTH) {
                throw IllegalStateException("Transaction depth exceeds current limit ($MAX_DEPTH)")
            }

            stack.push(stateController.currentState)
            return StateTransactionHelper(stateController, stack)
        }

        internal fun recordTransactionDuration(controller: StateController<*>) {
            performanceMonitor[controller] = System.currentTimeMillis()
        }
    }

    /**
     * 应用临时状态
     * @param state 需要设置的临时状态
     */
    fun applyState(state: T) {
        try {
            stateController.applyState(state)
            stateController.onStateChanged(state) // 触发回调
            _stateLiveData.postValue(state) // 更新LiveData
        } catch (ex: Exception) {
            handleApplyError(ex)
        }
    }

    /**
     * 恢复上一个状态（手动调用）
     */
    fun restore() {
        try {
            if (stateStack.isNotEmpty()) {
                val previousState = stateStack.pop()
                stateController.applyState(previousState)
                stateController.onStateChanged(previousState) // 触发回调
                _stateLiveData.postValue(previousState) // 更新LiveData
            }
        } catch (ex: Exception) {
            handleRestoreError(ex)
        }
    }

    private fun handleApplyError(ex: Exception) {
        // 异常处理逻辑
        restore()
        (stateController as? ErrorCallback)?.onRollbackError(ex)
    }

    private fun handleRestoreError(ex: Exception) {
        // 回滚失败处理逻辑
        (stateController as? ErrorCallback)?.onRollbackError(ex)
    }

    /**
     * 状态控制器接口
     */
    interface StateController<T> {
        val currentState: T
        fun applyState(state: T)
        // 新增状态变更回调
        fun onStateChanged(newState: T) = Unit
    }

    // 新增异常回调接口
    interface ErrorCallback {
        fun onRollbackError(ex: Exception)
    }


    /**
     * 获取当前事务深度（调试用）
     */
    fun transactionDepth(): Int = stateStack.size
}