package io.core.ui.helper.jetpack

import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * 增强版防数据倒灌LiveData
 * 核心特性：
 * 1. 精确生命周期感知 - 基于STARTED状态判�?
 * 2. 双重防抖机制 - 全局/观察者级�?
 * 3. 线程安全设计 - 使用ConcurrentHashMap和原子操�?
 * 4. 空值安全支�?- 显式声明可空类型
 * 5. 调试日志支持 - 可追踪数据流和观察者状�?
 *
 * @param initialValue 初始值（可选）
 * @param T 数据类型（可为空类型�?
 *
 * 使用约束�?
 * 1. setValue必须在主线程调用
 * 2. 防抖阈值设置需大于业务操作最小间�?
 * 3. 通过observe/observeForever注册观察�?
 */
class SingleLiveData<T> @JvmOverloads constructor(
    private val initialValue: T? = null
) : LiveData<T>() {

    // region 核心属�?
    // 更新挂起标志（原子操作保证线程安全）
    private val pending = AtomicBoolean(false)
    
    // 最后更新时间戳（用于全局防抖�?
    private val lastUpdateTime = AtomicLong(0)
    
    // 观察者包装器映射表（线程安全容器�?
    private val observerWrappers = ConcurrentHashMap<Observer<in T>, WrappedObserver>()
    
    // 全局防抖动阈值（单位：毫秒）
    private var debounceThreshold = 0L

    init {
        // 初始化设置初始值但不会触发观察�?
        initialValue?.let { super.setValue(it) }
    }
    // endregion

    // region 观察者包装类
    /**
     * 观察者包装器实现
     * 职责�?
     * 1. 管理观察者消费状�?
     * 2. 实现防抖动逻辑
     * 3. 生命周期状态检�?
     *
     * @param owner 生命周期拥有者（null表示永久观察�?
     * @param delegate 原始观察�?
     * @param debounceThreshold 观察者级别防抖阈�?
     */
    private inner class WrappedObserver(
        private val owner: LifecycleOwner?,
        private val delegate: Observer<in T>,
        private val debounceThreshold: Long
    ) : Observer<T> {

        // 消费状态标志（原子操作保证线程安全�?
        private val consumed = AtomicBoolean(false)
        
        // 最后消费时间戳（用于观察者级别防抖）
        private var lastConsumeTime = 0L

        /**
         * 数据变更回调
         * @param value 新数据�?
         */
        override fun onChanged(value: T) {
            if (shouldSkipUpdate()) return
            
            // 防抖动检查（同时满足全局和观察者级别防抖）
            if (checkDebounce() && pending.get() && consumed.compareAndSet(false, true)) {
                delegate.onChanged(value)
                lastConsumeTime = System.currentTimeMillis()
            }
        }

        /**
         * 是否跳过更新检�?
         * 条件：生命周期未达到STARTED状�?
         */
        private fun shouldSkipUpdate(): Boolean {
            return owner?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) == false
        }

        /**
         * 防抖动检�?
         * 条件：当前时间与最后消费时间差大于阈�?
         */
        private fun checkDebounce(): Boolean {
            return (System.currentTimeMillis() - lastConsumeTime) > debounceThreshold
        }

        /**
         * 重置消费状�?
         * 在每次新数据更新时调�?
         */
        fun resetConsumeState() {
            consumed.set(false)
        }
    }
    // endregion

    // region 公开API增强
    /**
     * 注册带防抖动的观察�?
     * @param owner 生命周期拥有�?
     * @param debounceMs 观察者级别防抖阈值（单位：毫秒）
     * @param observer 观察者实�?
     */
    @MainThread
    fun observe(
        owner: LifecycleOwner,
        debounceMs: Long = 0,
        observer: Observer<in T>
    ) {
        val wrapper = WrappedObserver(owner, observer, debounceMs)
        observerWrappers[observer] = wrapper
        super.observe(owner, wrapper)
    }

    /**
     * 简化版观察方法（lambda形式�?
     * @param owner 生命周期拥有�?
     * @param onChanged 数据变更回调
     */
    @MainThread
    fun observe(owner: LifecycleOwner, onChanged: (T) -> Unit) {
        observe(owner, 0) { onChanged(it) }
    }

    /**
     * 注册永久观察者（注意内存泄漏风险�?
     */
    @MainThread
    override fun observeForever(observer: Observer<in T>) {
        val wrapper = WrappedObserver(null, observer, 0)
        observerWrappers[observer] = wrapper
        super.observeForever(wrapper)
    }

    /**
     * 移除观察�?
     */
    @MainThread
    override fun removeObserver(observer: Observer<in T>) {
        observerWrappers.remove(observer)?.let {
            super.removeObserver(it)
        }
    }
    // endregion

    // region 值更新逻辑
    /**
     * 同步更新�?
     * @param value 新�?
     */
    @MainThread
    public override fun setValue(value: T) {
        if (shouldDispatch(value)) {
            prepareUpdate()
            super.setValue(value)
        }
    }

    /**
     * 异步更新�?
     * @param value 新�?
     */
    public override fun postValue(value: T) {
        if (shouldDispatch(value)) {
            prepareUpdate()
            super.postValue(value)
        }
    }

    /**
     * 准备更新操作
     * 1. 设置挂起标志
     * 2. 重置所有观察者的消费状�?
     * 3. 记录更新时间�?
     */
    private fun prepareUpdate() {
        pending.set(true)
        safelyResetConsumeStates()
        lastUpdateTime.set(System.currentTimeMillis())
    }

    /**
     * 安全重置消费状�?
     * 使用防御性复制避免并发修改异�?
     */
    private fun safelyResetConsumeStates() {
        // 创建副本避免遍历时发生修�?
        val wrappers = ArrayList(observerWrappers.values)
        wrappers.forEach { it.resetConsumeState() }
    }

    /**
     * 判断是否需要分发更�?
     * 条件：值发生改变或超过全局防抖阈�?
     */
    private fun shouldDispatch(newValue: T): Boolean {
        return (value != newValue) || (System.currentTimeMillis() - lastUpdateTime.get() > debounceThreshold)
    }
    // endregion

    // region 扩展功能
    /**
     * 设置全局防抖动阈�?
     * @param thresholdMs 阈值（毫秒�?
     * @return 当前实例（支持链式调用）
     */
    fun setDebounceThreshold(thresholdMs: Long): SingleLiveData<T> {
        this.debounceThreshold = thresholdMs
        return this
    }

    /**
     * 清除所有观察�?
     */
    @MainThread
    fun clearAllObservers() {
        observerWrappers.keys.toList().forEach { removeObserver(it) }
    }

    /**
     * 安全获取当前�?
     * @return 当前值或null
     */
    fun getSafeValue(): T? = value

    /**
     * 无活跃观察者时自动重置状�?
     */
    override fun onInactive() {
        super.onInactive()
        pending.set(false)
    }
    // endregion

    // region 调试支持
    /**
     * 调试模式开�?
     */
    var debugEnabled = false
        set(value) {
            field = value
            if (value) println("[SingleLiveData] 调试模式已启�?)
        }

    /**
     * 打印观察者信�?
     */
    fun printObservers() {
        if (debugEnabled) {
            println("当前观察者数�? ${observerWrappers.size}")
            observerWrappers.keys.forEachIndexed { i, obs ->
                println("观察�?$i: ${obs.javaClass.simpleName}")
            }
        }
    }
    // endregion
}
