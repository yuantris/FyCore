package io.core.engine.shape.drawable

import androidx.annotation.IntDef

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/21 9:54
 * @description
 * @author Yuan
 */
@IntDef(
    ShapeGradientType.LINEAR_GRADIENT,
    ShapeGradientType.RADIAL_GRADIENT,
    ShapeGradientType.SWEEP_GRADIENT
)
@Retention(AnnotationRetention.SOURCE)
annotation class ShapeGradientTypeLimit