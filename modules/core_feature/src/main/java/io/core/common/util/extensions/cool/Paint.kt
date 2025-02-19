package io.core.common.util.extensions.cool

import android.text.TextPaint

val TextPaint.textHeight: Float
    get() = fontMetrics.run { descent - ascent + leading }
