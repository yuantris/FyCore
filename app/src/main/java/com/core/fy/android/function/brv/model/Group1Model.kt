package com.core.fy.android.function.brv.model

import androidx.databinding.BaseObservable
import com.core.fy.android.R
import com.drake.brv.item.ItemExpand
import com.drake.brv.item.ItemHover
import com.drake.brv.item.ItemPosition

open class Group1Model : ItemExpand, ItemHover, ItemPosition,
    BaseObservable() {

    override var itemGroupPosition: Int = 0
    override var itemExpand: Boolean = false
        set(value) {
            field = value
            notifyChange()
        }

    override fun getItemSublist(): List<Any?>? {
        return sublist
    }

    var sublist: List<Any?> = MutableList(4) { Group3Model() }

    override var itemHover: Boolean = true
    override var itemPosition: Int = 0

    val title get() = "分组 [ $itemGroupPosition ]"

    val expandIcon get() = if (itemExpand) R.drawable.ic_arrow_expand else R.drawable.ic_arrow_collapse

}