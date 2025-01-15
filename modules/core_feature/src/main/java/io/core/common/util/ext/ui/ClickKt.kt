package io.core.common.util.ext.ui

import android.view.View

fun View.onClick(block: View.OnClickListener) = setOnClickListener(block)

inline fun View.onLongClick(
    consume: Boolean = true,
    crossinline block: () -> Unit
) = setOnLongClickListener { block(); consume }