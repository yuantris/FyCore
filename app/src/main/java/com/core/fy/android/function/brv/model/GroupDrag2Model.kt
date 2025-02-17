package com.core.fy.android.function.brv.model

import io.core.engine.brv.annotaion.ItemOrientation
import io.core.engine.brv.item.ItemSwipe


/** 为[Group3Model]添加侧滑功能 */
class GroupDrag2Model(
    override var itemOrientationSwipe: Int = ItemOrientation.HORIZONTAL, // 侧滑方向
) : Group3Model(), ItemSwipe