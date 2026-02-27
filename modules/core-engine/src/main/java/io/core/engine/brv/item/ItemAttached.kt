package io.core.engine.brv.item

import io.core.engine.brv.BindingAdapter

interface ItemAttached {

    fun onViewAttachedToWindow(holder: BindingAdapter.BindingViewHolder)

    fun onViewDetachedFromWindow(holder: BindingAdapter.BindingViewHolder)
}