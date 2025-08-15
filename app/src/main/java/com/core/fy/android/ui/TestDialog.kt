package com.core.fy.android.ui

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.core.fy.android.R
import com.core.fy.android.databinding.DialogTestBinding
import io.core.common.base.component.dialog.BaseDialogFragmentV2

class TestDialog: BaseDialogFragmentV2<DialogTestBinding>(R.layout.dialog_test) {

    override val widthRatio: Float
        get() = 1f

    override val isCancelableOutside: Boolean
        get() = false

    override fun applyAnimation(animation: DialogAnimation) {
        super.applyAnimation(DialogAnimation.BOTTOM)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setDimAmount(0.5f)
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }
}