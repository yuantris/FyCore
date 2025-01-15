@file:Suppress("unused")

package io.core.common.helper.dialogs

import android.content.Context
import android.content.DialogInterface

fun Context.selector(
    items: List<CharSequence>,
    onClick: (DialogInterface, Int) -> Unit
) {
    with(AndroidAlertBuilder(this)) {
        items(items, onClick)
        show()
    }
}

fun <T> Context.selector(
    items: List<T>,
    onClick: (DialogInterface, T, Int) -> Unit
) {
    with(AndroidAlertBuilder(this)) {
        items(items, onClick)
        show()
    }
}

fun Context.selector(
    title: CharSequence,
    items: List<CharSequence>,
    onClick: (DialogInterface, Int) -> Unit
) {
    with(AndroidAlertBuilder(this)) {
        this.setTitle(title)
        items(items, onClick)
        show()
    }
}

fun <T> Context.selector(
    title: CharSequence,
    items: List<T>,
    onClick: (DialogInterface, T, Int) -> Unit
) {
    with(AndroidAlertBuilder(this)) {
        this.setTitle(title)
        items(items, onClick)
        show()
    }
}

fun Context.selector(
    titleSource: Int,
    items: List<CharSequence>,
    onClick: (DialogInterface, Int) -> Unit
) {
    with(AndroidAlertBuilder(this)) {
        this.setTitle(titleSource)
        items(items, onClick)
        show()
    }
}

fun <T> Context.selector(
    titleSource: Int,
    items: List<T>,
    onClick: (DialogInterface, T, Int) -> Unit
) {
    with(AndroidAlertBuilder(this)) {
        this.setTitle(titleSource)
        items(items, onClick)
        show()
    }
}
