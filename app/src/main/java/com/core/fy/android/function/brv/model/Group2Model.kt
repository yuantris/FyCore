package com.core.fy.android.function.brv.model

import androidx.databinding.BaseObservable
import com.core.fy.android.R
import com.drake.brv.item.ItemExpand

class Group2Model : ItemExpand, BaseObservable() {

    override var itemGroupPosition: Int = 0
    override var itemExpand: Boolean = false
        set(value) {
            field = value
            notifyChange()
        }

    override fun getItemSublist(): List<Any?> {
        return MutableList(4) { Group3Model() }
    }

    val title get() = "嵌套分组 [ $itemGroupPosition ]"
    val expandIcon get() = if (itemExpand) R.drawable.ic_arrow_nested_expand else R.drawable.ic_arrow_nested_collapse
}