package io.core.common.base.action

interface AnimAction {

    /** 动画类型枚举 */
    enum class AnimationType {
        DEFAULT,
        EMPTY,
        SCALE,
        IOS,
        TOAST,
        TOP,
        BOTTOM,
        LEFT,
        RIGHT
    }

    companion object {

        /** 默认动画效果 */
        val ANIM_DEFAULT = AnimationType.DEFAULT

        /** 没有动画效果 */
        val ANIM_EMPTY = AnimationType.EMPTY

        /** 缩放动画 */
        val ANIM_SCALE = AnimationType.SCALE

        /** IOS 动画 */
        val ANIM_IOS = AnimationType.IOS

        /** 吐司动画 */
        val ANIM_TOAST = AnimationType.TOAST

        /** 顶部弹出动画 */
        val ANIM_TOP = AnimationType.TOP

        /** 底部弹出动画 */
        val ANIM_BOTTOM = AnimationType.BOTTOM

        /** 左边弹出动画 */
        val ANIM_LEFT = AnimationType.LEFT

        /** 右边弹出动画 */
        val ANIM_RIGHT = AnimationType.RIGHT
    }
}
