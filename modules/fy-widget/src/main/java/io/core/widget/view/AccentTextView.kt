package io.core.widget.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import io.core.widget.R
import io.core.common.util.extensions.ui.getCompatColor

class AccentTextView(context: Context, attrs: AttributeSet?) :
    AppCompatTextView(context, attrs) {

    init {
        if (!isInEditMode) {
            setTextColor(context.getCompatColor(R.color.common_accent_color))
        } else {
            setTextColor(context.getCompatColor(R.color.common_accent_color))
        }
    }

}
