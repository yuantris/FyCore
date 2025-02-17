@file:Suppress("unused")

package io.core.common.util.tools

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.fragment.app.Fragment
import io.core.BuildConfig

private var toast: Toast? = null

private var toastFunny: Toast? = null

fun Context.toastOnUi(message: Int, duration: Int = Toast.LENGTH_SHORT) {
    toastOnUi(getString(message), duration)
}

@SuppressLint("InflateParams")
fun Context.toastOnUi(message: CharSequence?, duration: Int = Toast.LENGTH_SHORT) {
    runOnUI {
        kotlin.runCatching {
            toast?.cancel()
            toast = Toast(this)
            toast?.setText(message)
            toast?.duration = duration
            toast?.show()
        }
    }
}

fun Context.toastOnUiFunny(message: CharSequence) {
    runOnUI {
        kotlin.runCatching {
            if (toastFunny == null || BuildConfig.DEBUG) {
                toastFunny = Toast.makeText(this, message, Toast.LENGTH_SHORT)
            } else {
                toastFunny?.setText(message)
                toastFunny?.duration = Toast.LENGTH_SHORT
            }
            toastFunny?.show()
        }
    }
}

fun Context.longToastOnUi(message: Int) {
    toastOnUi(message, Toast.LENGTH_LONG)
}

fun Context.longToastOnUi(message: CharSequence?) {
    toastOnUi(message, Toast.LENGTH_LONG)
}

fun Context.longToastOnUiFunny(message: CharSequence) {
    runOnUI {
        kotlin.runCatching {
            if (toastFunny == null || BuildConfig.DEBUG) {
                toastFunny = Toast.makeText(this, message, Toast.LENGTH_LONG)
            } else {
                toastFunny?.setText(message)
                toastFunny?.duration = Toast.LENGTH_LONG
            }
            toastFunny?.show()
        }
    }
}

fun Fragment.toastOnUi(message: Int) = requireActivity().toastOnUi(message)

fun Fragment.toastOnUi(message: CharSequence) = requireActivity().toastOnUi(message)

fun Fragment.longToast(message: Int) = requireContext().longToastOnUi(message)

fun Fragment.longToast(message: CharSequence) = requireContext().longToastOnUi(message)
