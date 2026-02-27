package io.core.ui.base.component.custom.strategy

import android.content.Context
import android.view.View

/**
 * 吐司外观策略接口
 * 定义吐司的视觉外观创建方�?
 */
interface ToastAppearanceStrategy {
    /**
     * 创建吐司视图
     * @param context 上下�?
     * @param message 显示消息
     * @return 配置好的视图
     */
    fun createToastView(context: Context, message: String): View

    /**
     * 获取默认布局资源ID
     */
    fun getDefaultLayoutId(): Int

    /**
     * 获取默认文本视图ID
     */
    fun getDefaultTextViewId(): Int
}