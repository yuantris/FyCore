package io.core.common.util.tools

class MultiLineBuilder {
    private val lines = mutableListOf<String>()

    fun append(line: String) {
        lines.add(line)
    }

    /**
     * 批量添加多行
     */
    fun appendLines(vararg lines: String) {
        this.lines.addAll(lines)
    }

    /**
     * 添加带缩进的行
     */
    fun appendIndent(indent: Int, line: String) {
        lines.add(" ".repeat(indent) + line)
    }

    /**
     * 添加空行
     */
    fun appendBlank() {
        lines.add("")
    }

    /**
     * 添加带前缀的行 (如注释符号、项目符号)
     */
    fun appendWithPrefix(prefix: String, line: String) {
        lines.add("$prefix$line")
    }

    /**
     * 添加分隔线
     */
    fun appendDivider(length: Int, char: Char = '-') {
        lines.add(char.toString().repeat(length))
    }

    internal fun build(): String {
        return lines.joinToString("\n") {
            if (it.isBlank()) it else it.trimEnd() // 自动去除行尾空格(空行保留)
        }
    }
}

fun buildMultiLine(block: MultiLineBuilder.() -> Unit): String {
    val builder = MultiLineBuilder()
    builder.block()
    return builder.build()
}