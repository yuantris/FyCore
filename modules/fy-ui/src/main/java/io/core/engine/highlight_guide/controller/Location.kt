package io.core.engine.highlight_guide.controller

/**
 * Desc: 附加视图的位置
 * TO_XXX可以和ALIGN_XXX同时使用
 *
 * Date: 2025/1/23 10:53
 */
enum class Location {
    COVER,
    TO_LEFT,
    TO_RIGHT,
    TO_TOP,
    TO_BOTTOM,
    ALIGN_TOP,
    ALIGN_BOTTOM,
    ALIGN_LEFT,
    ALIGN_RIGHT,
    ALIGN_PARENT_RIGHT;
}