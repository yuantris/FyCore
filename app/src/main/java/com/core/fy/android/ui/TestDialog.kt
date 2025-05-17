package com.core.fy.android.ui

import android.os.Bundle
import com.core.fy.android.R
import com.core.fy.android.databinding.DialogTestBinding
import com.core.fy.android.databinding.FragmentKotlinBinding
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
        dialog?.window?.setDimAmount(0.2f)
    }
}