package io.core.common.base.component.custom.defaultconfig

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import io.core.ui.R
import io.core.common.base.component.custom.strategy.ToastAppearanceStrategy

/**
 * 默认吐司外观实现
 */
class DefaultToastAppearance : ToastAppearanceStrategy {
    override fun createToastView(context: Context, message: String): View {
        return LayoutInflater.from(context)
            .inflate(getDefaultLayoutId(), null).apply {
                findViewById<TextView>(getDefaultTextViewId()).text = message
            }
    }

    override fun getDefaultLayoutId(): Int = R.layout.layout_blut_toast

    override fun getDefaultTextViewId(): Int = R.id.tv_message
}