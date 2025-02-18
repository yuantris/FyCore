@file:Suppress("unused")

package io.core.common.util.ext.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.view.Menu
import androidx.annotation.ColorInt
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.forEach
import io.core.R

@SuppressLint("RestrictedApi")
fun Menu.applyTint(context: Context): Menu = this.let { menu ->
    if (menu is MenuBuilder) {
        menu.setOptionalIconsVisible(true)
    }
    val defaultTextColor = context.getCompatColor(R.color.primaryText)
    val tintColor = MenuExtensions.getMenuColor(context)
    menu.forEach { item ->
        (item as MenuItemImpl).let { impl ->
            //overflow：展开的item
            impl.icon?.setTintMutate(
                if (impl.requiresOverflow()) defaultTextColor else tintColor
            )
        }
    }
    return menu
}


fun Drawable.setTintMutate(
    @ColorInt tint: Int,
    tintMode: PorterDuff.Mode = PorterDuff.Mode.SRC_ATOP
) {
    val wrappedDrawable = DrawableCompat.wrap(this)
    wrappedDrawable.mutate()
    DrawableCompat.setTintMode(wrappedDrawable, tintMode)
    DrawableCompat.setTint(wrappedDrawable, tint)
}

object MenuExtensions {

    fun getMenuColor(
        context: Context,
        requiresOverflow: Boolean = false
    ): Int {
        val defaultTextColor = context.getCompatColor(R.color.primaryText)
        if (requiresOverflow)
            return defaultTextColor
        val primaryTextColor = context.getCompatColor(R.color.common_text_color)
        return primaryTextColor
    }

}
