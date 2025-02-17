package com.core.fy.android.function.brv.model

import io.core.engine.brv.item.ItemExpand


open class Group3Model(
    override var itemGroupPosition: Int = 0,
    override var itemExpand: Boolean = false,
) : ItemExpand {
    override fun getItemSublist(): List<Any>? {
        return null
    }
}