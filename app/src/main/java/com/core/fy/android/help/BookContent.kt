package com.core.fy.android.help


data class BookContent(
    val sameTitleRemoved: Boolean,
    val textList: List<String>,
    //起效的替换规�?
    //val effectiveReplaceRules: List<ReplaceRule>?
) {

    override fun toString(): String {
        return textList.joinToString("\n")
    }

}
