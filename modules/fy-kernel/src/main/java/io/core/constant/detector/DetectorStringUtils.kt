package io.core.constant.detector

// kernel inline string utils (replaces dependency on fy-utils StringTools/extensions)
internal fun String.removeWhitespace(): String = replace("\\s+".toRegex(), "")

internal fun extractNumber(input: String): String = input.replace(Regex("[^0-9.]"), "")
