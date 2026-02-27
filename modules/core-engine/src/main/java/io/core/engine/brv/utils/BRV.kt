package io.core.engine.brv.utils

object BRV {

    /**
     * 即item的layout布局中的<variable>标签内定义变量名�?
     * 示例:
     * ```
     * <variable
     *      name="m"
     *      type="io.core.engine.brv.sample.model.CheckModel" />
     * ```
     * 则应在Application中的onCreate函数内设�?
     * `BRV.modelId = BR.m`
     */
    var modelId: Int = -1

    /**
     * 防抖动点击事件默认的间隔时间, 单位毫秒
     * @see io.core.engine.brv.BindingAdapter.onClick
     */
    var debounceClickInterval: Long = 500

    @Deprecated("命名规范", ReplaceWith("BRV.debounceClickInterval"), DeprecationLevel.ERROR)
    var clickThrottle: Long = 500

    /**
     * 启用item所有view点击事件共享防抖动间�? BRV默认防抖动仅针对单个view
     * @see io.core.engine.brv.BindingAdapter.onClick
     */
    var debounceGlobalEnabled: Boolean = false

    /**
     * 此变量用于支持全局控件防抖�?
     * @see io.core.engine.brv.BindingAdapter.onClick
     * @see debounceGlobalEnabled 要求先启用全局共享防抖�?
     */
    var lastDebounceClickTime: Long = 0
}