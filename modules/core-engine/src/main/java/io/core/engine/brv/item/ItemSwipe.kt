package io.core.engine.brv.item

import io.core.engine.brv.annotaion.ItemOrientation

/**
 * 可侧滑的条目
 */
interface ItemSwipe {

    /**
     * 侧滑方向
     * @see ItemOrientation
     */
    var itemOrientationSwipe: Int
}