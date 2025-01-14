package io.core.common.base.action

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat

interface ResourcesAction {

    fun getContext(): Context

    fun getResources(): Resources {
        return getContext().resources
    }

    fun getString(@StringRes id: Int): String? {
        return getContext().getString(id)
    }

    fun getString(@StringRes id: Int, vararg formatArgs: Any?): String? {
        return getResources().getString(id, *formatArgs)
    }

    fun getDrawable(@DrawableRes id: Int): Drawable? {
        return ContextCompat.getDrawable(getContext(), id)
    }

    @ColorInt
    fun getColor(@ColorRes id: Int): Int {
        return ContextCompat.getColor(getContext(), id)
    }

    fun <S> getSystemService(serviceClass: Class<S>): S {
        return ContextCompat.getSystemService(getContext(), serviceClass)!!
    }
}