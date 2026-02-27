package com.core.fy.android.ui

import com.core.fy.android.R
import com.core.fy.android.databinding.DialogCommonBinding
import io.core.ui.base.component.dialog.BaseDialogFragmentV2
import io.core.utils.extensions.ui.onClick

class TestV2Dialog: BaseDialogFragmentV2<DialogCommonBinding>(R.layout.dialog_common) {

    override fun initView() {
        super.initView()


        binding.tvCancel.onClick { dismiss() }
        binding.tvConfirm.onClick { dismiss() }
    }
}