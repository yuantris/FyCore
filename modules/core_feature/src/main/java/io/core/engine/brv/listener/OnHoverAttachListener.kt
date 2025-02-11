package io.core.engine.brv.listener

import android.view.View

/**
 * 监听Item附着/分离屏幕顶部回调
 */
interface OnHoverAttachListener {
    /**
     * 当条目附着时
     * [detachHover] 该函数可以进行还原
     */
    fun attachHover(v: View)

    /**
     * 条目分离时
     * 一般用于还原[attachHover]函数
     */
    fun detachHover(v: View)
}