package io.core.common.util.ext.ui

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.view.forEach
import androidx.fragment.app.DialogFragment
import io.core.R
import io.core.common.util.ext.cool.dpToPx
import io.core.common.util.ext.windowManager

val Context.filletBackground: GradientDrawable
    get() {
        val background = GradientDrawable()
        background.cornerRadius = 16f.dpToPx()
        background.setColor(getColor(R.color.common_window_background_color))
        return background
    }

fun AlertDialog.applyTint(): AlertDialog {
    window?.setBackgroundDrawable(context.filletBackground)
//    val colorStateList = Selector.colorBuild()
//        .setDefaultColor(ThemeStore.accentColor(context))
//        .setPressedColor(ColorUtils.darkenColor(ThemeStore.accentColor(context)))
//        .create()
//    if (getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
//        getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(colorStateList)
//    }
//    if (getButton(AlertDialog.BUTTON_POSITIVE) != null) {
//        getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(colorStateList)
//    }
//    if (getButton(AlertDialog.BUTTON_NEUTRAL) != null) {
//        getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(colorStateList)
//    }
//    window?.decorView?.post {
//        listView?.forEach {
//            it.applyTint(context.accentColor)
//        }
//    }
    return this
}

fun AlertDialog.requestInputMethod() {
    window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
}

fun DialogFragment.setLayout(widthMix: Float, heightMix: Float) {
    dialog?.setLayout(widthMix, heightMix)
}

fun Dialog.setLayout(widthMix: Float, heightMix: Float) {
    val dm = context.windowManager.windowSize
    window?.setLayout(
        (dm.widthPixels * widthMix).toInt(),
        (dm.heightPixels * heightMix).toInt()
    )
}

fun DialogFragment.setLayout(width: Int, heightMix: Float) {
    dialog?.setLayout(width, heightMix)
}

fun Dialog.setLayout(width: Int, heightMix: Float) {
    val dm = context.windowManager.windowSize
    window?.setLayout(
        width,
        (dm.heightPixels * heightMix).toInt()
    )
}

fun DialogFragment.setLayout(widthMix: Float, height: Int) {
    dialog?.setLayout(widthMix, height)
}

fun Dialog.setLayout(widthMix: Float, height: Int) {
    val dm = context.windowManager.windowSize
    window?.setLayout(
        (dm.widthPixels * widthMix).toInt(),
        height
    )
}

fun DialogFragment.setLayout(width: Int, height: Int) {
    dialog?.setLayout(width, height)
}

fun Dialog.setLayout(width: Int, height: Int) {
    window?.setLayout(width, height)
}