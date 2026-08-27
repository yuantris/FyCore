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
 * 2025/1/21 9:49
 * @description
 * @author Yuan
 */
@IntDef(ShapeType.RECTANGLE, ShapeType.OVAL, ShapeType.LINE, ShapeType.RING)
@Retention(AnnotationRetention.SOURCE)
annotation class ShapeTypeLimit