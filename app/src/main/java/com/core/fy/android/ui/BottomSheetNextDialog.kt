package com.core.fy.android.ui

import com.core.fy.android.R
import com.core.fy.android.databinding.DialogBottomStreetBinding
import io.core.common.base.component.dialog.BaseBottomSheetDialog
import io.core.common.util.extensions.ui.getCompatColor
import io.core.common.util.tools.DrawableBuilder

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/17 9:47
 * @description
 * @author Yuan
 */
class BottomSheetNextDialog : BaseBottomSheetDialog<DialogBottomStreetBinding>() {

    override fun initConfig(builder: Builder) {
        val compatColor = getCompatColor(R.color.color_white)
        val drawable = DrawableBuilder
            .setTopLeftRadius(16f)
            .setTopRightRadius(16f)
            .setSolidColor(compatColor)
            .build()
        builder.setBackground(drawable)
        builder.setPadding(20, 20, 20, 20)
    }

    override fun initView() {
        super.initView()

    }
}