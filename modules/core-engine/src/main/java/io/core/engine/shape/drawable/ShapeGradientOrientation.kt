package io.core.engine.shape.drawable

/**
# ██████████
# █▄█████▄�?
# █▼▼▼▼▼
# �?
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载…�?
 * 2025/1/21 9:55
 * @description Shape 渐变方向
 * @author Yuan
 */
enum class ShapeGradientOrientation {

    /** 从左到右绘制渐变�? 度） */
    LEFT_TO_RIGHT,
    START_TO_END,

    /** 从右到左绘制渐变�?80 度） */
    RIGHT_TO_LEFT,
    END_TO_START,

    /** 从下到上绘制渐变�?0 度） */
    BOTTOM_TO_TOP,

    /** 从上到下绘制渐变�?70 度） */
    TOP_TO_BOTTOM,

    // ------------------------------ //

    /** 从左上角到右下角绘制渐变�?15 度） */
    TOP_LEFT_TO_BOTTOM_RIGHT,
    TOP_START_TO_BOTTOM_END,

    /** 从右上角到左下角绘制渐变�?25 度） */
    TOP_RIGHT_TO_BOTTOM_LEFT,
    TOP_END_TO_BOTTOM_START,

    /** 从左下角到右上角绘制渐变�?5 度） */
    BOTTOM_LEFT_TO_TOP_RIGHT,
    BOTTOM_START_TO_TOP_END,

    /** 从右下角到左上角绘制渐变�?35 度） */
    BOTTOM_RIGHT_TO_TOP_LEFT,
    BOTTOM_END_TO_TOP_START
}