package io.core.utils.extensions.cool

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.Fragment
import io.core.utils.extensions.ui.ctx

fun Context.toastOnUI(message: Int, duration: Int = Toast.LENGTH_SHORT) {
    toastOnUI(getString(message), duration)
}

fun Context.toastOnUI(message: CharSequence?, duration: Int = Toast.LENGTH_SHORT) = runMain {
    runCatching {
        Toast.makeText(this.applicationContext, message, duration).show()
    }
}


fun Context.longToastOnUI(message: Int) {
    toastOnUI(message, Toast.LENGTH_LONG)
}

fun Context.longToastOnUI(message: CharSequence?) {
    toastOnUI(message, Toast.LENGTH_LONG)
}


fun Fragment.toastOnUI(message: Int) = ctx.toastOnUI(message)

fun Fragment.toastOnUI(message: CharSequence) = ctx.toastOnUI(message)

fun Fragment.longToast(message: Int) = ctx.longToastOnUI(message)

fun Fragment.longToast(message: CharSequence) = ctx.longToastOnUI(message)