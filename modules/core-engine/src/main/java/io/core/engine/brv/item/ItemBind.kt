package io.core.engine.brv.item

import io.core.engine.brv.BindingAdapter

interface ItemBind {
    fun onBind(vh: BindingAdapter.BindingViewHolder)
}