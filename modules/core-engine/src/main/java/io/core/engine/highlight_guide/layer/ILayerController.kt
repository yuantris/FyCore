package io.core.engine.highlight_guide.layer

/**
 * Desc: 蒙层控制器接�?
 *
 * Date: 2025/1/21 15:02
 */
internal interface ILayerController {
    /**
     * 显示蒙层
     */
    fun show()

    /**
     * 显示下一个蒙�?
     */
    fun showNext()

    /**
     * 关闭当前蒙层
     */
    fun dismissCurrent()
}
