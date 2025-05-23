package io.core.engine.brv.utils

import android.view.ViewGroup
import io.core.common.util.extensions.ui.setPaddingBottom
import io.core.common.util.tools.SizeTools
import io.core.engine.brv.BindingAdapter

fun BindingAdapter.BindingViewHolder.footDiveLineAdaptation(
    models: List<Any?>?,
    root: ViewGroup,
    ignoreOriginalPB: Boolean = false
) {
    models?.takeIf { modelPosition == it.size - 1 }?.let {
        root.setPaddingBottom((if (ignoreOriginalPB) 0 else root.paddingBottom) + SizeTools.getNavigationBarHeight())
    } ?: run { root.setPaddingBottom(if (ignoreOriginalPB) 0 else root.paddingBottom) }
}