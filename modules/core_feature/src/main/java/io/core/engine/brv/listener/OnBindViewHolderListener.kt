
package io.core.engine.brv.listener

import androidx.recyclerview.widget.RecyclerView
import io.core.engine.brv.BindingAdapter

/**
 * 实现[RecyclerView.Adapter.onBindViewHolder]接口回调
 */
interface OnBindViewHolderListener {
    fun onBindViewHolder(
        rv: RecyclerView,
        adapter: BindingAdapter,
        holder: BindingAdapter.BindingViewHolder,
        position: Int
    )
}