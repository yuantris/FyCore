package io.core.engine.brv.item

import io.core.engine.brv.annotaion.ItemOrientation

/**
 * 可拖拽
 */
interface ItemDrag {
    /**
     * 拖拽方向
     * @see ItemOrientation
     */
    var itemOrientationDrag: Int
}