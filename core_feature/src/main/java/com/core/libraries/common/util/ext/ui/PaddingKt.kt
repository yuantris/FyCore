package com.core.libraries.common.util.ext.ui

import android.os.Build.VERSION.SDK_INT
import android.view.View
import androidx.annotation.Px


inline var View.topPadding: Int
    get() = paddingTop
    set(@Px value) = setPadding(paddingLeft, value, paddingRight, paddingBottom)

inline var View.bottomPadding: Int
    get() = paddingBottom
    set(@Px value) = setPadding(paddingLeft, paddingTop, paddingRight, value)

inline var View.startPadding: Int
    get() = if (SDK_INT >= 17) paddingStart else paddingLeft
    set(@Px value) = when {
        SDK_INT >= 17 -> setPaddingRelative(value, paddingTop, paddingEnd, paddingBottom)
        else -> setPadding(value, paddingTop, paddingRight, paddingBottom)
    }

inline var View.endPadding: Int
    get() = if (SDK_INT >= 17) paddingEnd else paddingRight
    set(@Px value) = when {
        SDK_INT >= 17 -> setPaddingRelative(paddingStart, paddingTop, value, paddingBottom)
        else -> setPadding(paddingLeft, paddingTop, value, paddingBottom)
    }

inline var View.leftPadding: Int
    get() = paddingLeft
    set(@Px value) = setPadding(value, paddingTop, paddingRight, paddingBottom)

inline var View.rightPadding: Int
    get() = paddingRight
    set(@Px value) = setPadding(paddingLeft, paddingTop, value, paddingBottom)

