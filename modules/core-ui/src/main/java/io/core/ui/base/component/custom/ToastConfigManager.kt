package io.core.ui.base.component.custom

import io.core.ui.base.component.custom.defaultconfig.DefaultToastAnimation
import io.core.ui.base.component.custom.defaultconfig.DefaultToastAppearance
import io.core.ui.base.component.custom.defaultconfig.DefaultToastPosition
import io.core.ui.base.component.custom.strategy.ToastAnimationStrategy
import io.core.ui.base.component.custom.strategy.ToastAppearanceStrategy
import io.core.ui.base.component.custom.strategy.ToastPositionStrategy

/**
 * 吐司配置管理�?
 * 管理项目级默认的吐司配置
 */
object ToastConfigManager {

    // 默认策略实例
    private var defaultAppearance: ToastAppearanceStrategy = DefaultToastAppearance()
    private var defaultAnimation: ToastAnimationStrategy = DefaultToastAnimation()
    private var defaultPosition: ToastPositionStrategy = DefaultToastPosition()

    /**
     * 设置默认外观策略
     */
    fun setDefaultAppearance(appearance: ToastAppearanceStrategy) {
        defaultAppearance = appearance
    }

    /**
     * 设置默认动画策略
     */
    fun setDefaultAnimation(animation: ToastAnimationStrategy) {
        defaultAnimation = animation
    }

    /**
     * 设置默认位置策略
     */
    fun setDefaultPosition(position: ToastPositionStrategy) {
        defaultPosition = position
    }

    /**
     * 获取默认外观策略
     */
    fun getDefaultAppearance(): ToastAppearanceStrategy = defaultAppearance

    /**
     * 获取默认动画策略
     */
    fun getDefaultAnimation(): ToastAnimationStrategy = defaultAnimation

    /**
     * 获取默认位置策略
     */
    fun getDefaultPosition(): ToastPositionStrategy = defaultPosition

    /**
     * 重置为默认配�?
     */
    fun resetToDefaults() {
        defaultAppearance = DefaultToastAppearance()
        defaultAnimation = DefaultToastAnimation()
        defaultPosition = DefaultToastPosition()
    }
}