package com.core.fy.android.function.toast

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.core.fy.android.R
import io.core.common.base.component.custom.strategy.ToastAppearanceStrategy

class RedToastAppearance : ToastAppearanceStrategy {
    override fun createToastView(context: Context, message: String): View {
        return LayoutInflater.from(context)
            .inflate(getDefaultLayoutId(), null).apply {
                findViewById<TextView>(getDefaultTextViewId()).text = message
                findViewById<TextView>(getDefaultTextViewId()).setTextColor(context.getColor(R.color.white))
            }
    }

    override fun getDefaultLayoutId(): Int = R.layout.layout_blut_toast_app
    override fun getDefaultTextViewId(): Int = R.id.tv_message
}