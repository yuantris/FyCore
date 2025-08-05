package io.core.common.base.component.custom.strategy

import android.content.Context
import android.view.WindowManager

/**
 * 吐司位置策略接口
 * 定义吐司的显示位置和布局参数
 */
interface ToastPositionStrategy {
    /**
     * 创建布局参数
     * @param context 上下文
     * @return WindowManager.LayoutParams
     */
    fun createLayoutParams(context: Context): WindowManager.LayoutParams

    /**
     * 获取默认的位置
     */
    fun getDefaultGravity(): Int
}