@file:Suppress("NOTHING_TO_INLINE", "unused", "DEPRECATION")

package io.core.common.helper.dialogs

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.view.WindowManager
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import io.core.R
import io.core.common.helper.TryCatchHelper
import io.core.common.util.ext.cool.dpToPx

fun Context.showDialog(
    title: CharSequence? = null,
    message: CharSequence? = null,
    init: (AlertBuilder<DialogInterface>.() -> Unit)? = null
): AlertDialog {
    return AndroidAlertBuilder(this).apply {
        if (title != null) {
            this.setTitle(title)
        }
        if (message != null) {
            this.setMessage(message)
        }
        if (init != null) init()
    }.show()
}

inline fun Fragment.showDialog(
    title: CharSequence? = null,
    message: CharSequence? = null,
    noinline init: (AlertBuilder<DialogInterface>.() -> Unit)? = null
) = requireActivity().showDialog(title, message, init)

fun Context.showDialog(
    titleResource: Int? = null,
    messageResource: Int? = null,
    init: (AlertBuilder<DialogInterface>.() -> Unit)? = null
): AlertDialog {
    return AndroidAlertBuilder(this).apply {
        if (titleResource != null) {
            this.setTitle(titleResource)
        }
        if (messageResource != null) {
            this.setMessage(messageResource)
        }
        if (init != null) init()
    }.show()
}

inline fun Fragment.showDialog(
    titleResource: Int? = null,
    messageResource: Int? = null,
    noinline init: (AlertBuilder<DialogInterface>.() -> Unit)? = null
) = requireActivity().showDialog(titleResource, messageResource, init)

fun Context.showDialog(init: AlertBuilder<AlertDialog>.() -> Unit): AlertDialog =
    AndroidAlertBuilder(this).apply {
        init()
    }.show()

inline fun Fragment.showDialog(noinline init: AlertBuilder<DialogInterface>.() -> Unit) =
    requireContext().showDialog(init)

inline fun Fragment.progressDialog(
    title: Int? = null,
    message: Int? = null,
    noinline init: (ProgressDialog.() -> Unit)? = null
) = requireActivity().progressDialog(title, message, init)

fun Context.progressDialog(
    title: Int? = null,
    message: Int? = null,
    init: (ProgressDialog.() -> Unit)? = null
) = progressDialog(title?.let { getString(it) }, message?.let { getString(it) }, false, init)


inline fun Fragment.indeterminateProgressDialog(
    title: Int? = null,
    message: Int? = null,
    noinline init: (ProgressDialog.() -> Unit)? = null
) = requireActivity().indeterminateProgressDialog(title, message, init)

fun Context.indeterminateProgressDialog(
    title: Int? = null,
    message: Int? = null,
    init: (ProgressDialog.() -> Unit)? = null
) = progressDialog(title?.let { getString(it) }, message?.let { getString(it) }, true, init)

inline fun Fragment.progressDialog(
    title: CharSequence? = null,
    message: CharSequence? = null,
    noinline init: (ProgressDialog.() -> Unit)? = null
) = requireActivity().progressDialog(title, message, init)

fun Context.progressDialog(
    title: CharSequence? = null,
    message: CharSequence? = null,
    init: (ProgressDialog.() -> Unit)? = null
) = progressDialog(title, message, false, init)


inline fun Fragment.indeterminateProgressDialog(
    title: CharSequence? = null,
    message: CharSequence? = null,
    noinline init: (ProgressDialog.() -> Unit)? = null
) = requireActivity().indeterminateProgressDialog(title, message, init)

fun Context.indeterminateProgressDialog(
    title: CharSequence? = null,
    message: CharSequence? = null,
    init: (ProgressDialog.() -> Unit)? = null
) = progressDialog(title, message, true, init)


@SuppressLint("PrivateApi")
private fun Context.progressDialog(
    title: CharSequence? = null,
    message: CharSequence? = null,
    indeterminate: Boolean,
    init: (ProgressDialog.() -> Unit)? = null
) = ProgressDialog(this,R.style.CustomProgressDialog).apply {
    isIndeterminate = indeterminate
    if (!indeterminate) setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
    if (message != null) setMessage(message)
    if (title != null) setTitle(title)
    if (init != null) init()
    show()
}

typealias AlertBuilderFactory<D> = (Context) -> AlertBuilder<D>

inline fun <D : DialogInterface> Fragment.alert(
    noinline factory: AlertBuilderFactory<D>,
    title: String? = null,
    message: String? = null,
    noinline init: (AlertBuilder<D>.() -> Unit)? = null
) = activity?.alert(factory, title, message, init)

fun <D : DialogInterface> Context.alert(
    factory: AlertBuilderFactory<D>,
    title: String? = null,
    message: String? = null,
    init: (AlertBuilder<D>.() -> Unit)? = null
): AlertBuilder<D> {
    return factory(this).apply {
        if (title != null) {
            this.setTitle(title)
        }
        if (message != null) {
            this.setMessage(message)
        }
        if (init != null) init()
    }
}

inline fun <D : DialogInterface> Fragment.alert(
    noinline factory: AlertBuilderFactory<D>,
    titleResource: Int? = null,
    messageResource: Int? = null,
    noinline init: (AlertBuilder<D>.() -> Unit)? = null
) = requireActivity().alert(factory, titleResource, messageResource, init)

fun <D : DialogInterface> Context.alert(
    factory: AlertBuilderFactory<D>,
    titleResource: Int? = null,
    messageResource: Int? = null,
    init: (AlertBuilder<D>.() -> Unit)? = null
): AlertBuilder<D> {
    return factory(this).apply {
        if (titleResource != null) {
            this.setTitle(titleResource)
        }
        if (messageResource != null) {
            this.setMessage(messageResource)
        }
        if (init != null) init()
    }
}

inline fun <D : DialogInterface> Fragment.alert(
    noinline factory: AlertBuilderFactory<D>,
    noinline init: AlertBuilder<D>.() -> Unit
) = requireActivity().alert(factory, init)

fun <D : DialogInterface> Context.alert(
    factory: AlertBuilderFactory<D>,
    init: AlertBuilder<D>.() -> Unit
): AlertBuilder<D> = factory(this).apply { init() }
