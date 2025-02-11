package io.core.common.base.component.dialog

import android.animation.ValueAnimator
import android.content.Context
import android.content.DialogInterface
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.core.common.CoreConfig
import io.core.common.util.tools.OsUtils

class BaseAlertDialogBuilder : MaterialAlertDialogBuilder {
    constructor(context: Context) : super(context)
    constructor(context: Context, overrideThemeResId: Int) : super(context, overrideThemeResId)

    override fun create(): AlertDialog {
        return super.create().also { dialog ->
            val window = dialog.window ?: return@also
            if (OsUtils.atLeastS()) {
                window.apply {
                    addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

                    if (OsUtils.atLeastT()) {
                        val animator = ValueAnimator.ofInt(0, 13).apply {
                            duration = ANIMATION_DURATION
                            addUpdateListener { animation ->
                                if (!decorView.isAttachedToWindow) {
                                    cancel()
                                    return@addUpdateListener
                                }
                                val blurRadius =
                                    (animation.animatedValue as Int * BLUR_RADIUS_MULTIPLIER).coerceAtMost(
                                        MAX_BLUR_RADIUS
                                    )
                                attributes.blurBehindRadius = blurRadius
                                attributes = attributes
                            }
                            dialog.setOnDismissListener { cancel() }
                        }

                        dialog.setOnShowListener {
                            animator.start()
                            setButtonColors(dialog)
                        }
                    } else {
                        attributes.blurBehindRadius = DEFAULT_BLUR_RADIUS
                    }
                }
            } else {
                dialog.setOnShowListener {
                    setButtonColors(dialog)
                }
            }
        }
    }

    private fun setButtonColors(dialog: AlertDialog) {
        dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            ?.setTextColor(CoreConfig.DIALOG_BUTTON_POSITIVE_COLOR)
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
            ?.setTextColor(CoreConfig.DIALOG_BUTTON_NEGATIVE_COLOR)
    }

    companion object {
        private const val ANIMATION_DURATION = 350L
        private const val BLUR_RADIUS_MULTIPLIER = 5
        private const val MAX_BLUR_RADIUS = 64
        private const val DEFAULT_BLUR_RADIUS = 64
    }
}
