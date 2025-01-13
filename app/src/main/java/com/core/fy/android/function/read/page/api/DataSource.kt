package com.core.fy.android.function.read.page.api

import com.core.fy.android.function.read.model.ReadBook
import com.core.fy.android.function.read.page.entities.TextChapter


interface DataSource {

    val pageIndex: Int get() = ReadBook.durPageIndex

    val currentChapter: TextChapter?

    val nextChapter: TextChapter?

    val prevChapter: TextChapter?

    val isScroll: Boolean

    fun hasNextChapter(): Boolean

    fun hasPrevChapter(): Boolean

    fun upContent(relativePosition: Int = 0, resetPageOffset: Boolean = true)

}