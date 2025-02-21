package io.core.common.util.tools

class MultiLineBuilder {
    private val lines = mutableListOf<String>()

    fun append(line: String) {
        lines.add(line)
    }

    internal fun build(): String {
        return lines.joinToString("\n")
    }
}

fun buildMultiLine(block: MultiLineBuilder.() -> Unit): String {
    val builder = MultiLineBuilder()
    builder.block()
    return builder.build()
}