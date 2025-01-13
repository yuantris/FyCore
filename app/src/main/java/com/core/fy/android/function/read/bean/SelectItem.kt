package com.core.fy.android.function.read.bean

@Suppress("unused")
data class SelectItem<T>(
    val title: String,
    val value: T
) {

    override fun toString(): String {
        return title
    }

}
