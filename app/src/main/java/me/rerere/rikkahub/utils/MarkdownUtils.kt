package me.rerere.rikkahub.utils

private val inlineLatexRegex = Regex("\\\\\\((.+?)\\\\\\)")
private val blockLatexRegex = Regex("\\\\\\[(.+?)\\\\\\]", RegexOption.DOT_MATCHES_ALL)
private val codeBlockRegex = Regex("```[\\s\\S]*?```|`[^`\n]*`", RegexOption.DOT_MATCHES_ALL)
private val displayBracketOpenRegex = Regex("""\\\[""")
private val displayBracketCloseRegex = Regex("""\\]""")

/**
 * 移除字符串中的Markdown格式
 * @return 移除Markdown格式后的纯文本
 */
fun String.stripMarkdown(): String {
    return this
        // 移除代码块 (```...``` 和 `...`)
        .replace(Regex("```[\\s\\S]*?```|`[^`]*?`"), "")
        // 移除图片和链接，但保留其文本内容
        .replace(Regex("!?\\[([^\\]]+)\\]\\([^\\)]*\\)"), "$1")
        // 移除加粗和斜体 (先处理两个星号的)
        .replace(Regex("\\*\\*([^*]+?)\\*\\*"), "$1")
        .replace(Regex("\\*([^*]+?)\\*"), "$1")
        // 移除下划线
        .replace(Regex("__([^_]+?)__"), "$1")
        .replace(Regex("_([^_]+?)_"), "$1")
        // 移除删除线
        .replace(Regex("~~([^~]+?)~~"), "$1")
        // 移除标题标记 (多行模式)
        .replace(Regex("(?m)^#+\\s*"), "")
        // 移除列表标记 (多行模式)
        .replace(Regex("(?m)^\\s*[-*+]\\s+"), "")
        .replace(Regex("(?m)^\\s*\\d+\\.\\s+"), "")
        // 移除引用标记 (多行模式)
        .replace(Regex("(?m)^>\\s*"), "")
        // 移除水平分割线
        .replace(Regex("(?m)^(\\s*[-*_]){3,}\\s*$"), "")
        // 将多个换行符压缩，以保留段落
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}

fun String.extractThinkingTitle(): String? {
    // 按行分割文本
    val lines = this.lines()

    // 从后往前查找最后一个符合条件的加粗文本行
    for (i in lines.indices.reversed()) {
        val line = lines[i].trim()

        // 检查是否为加粗格式且独占一整行
        val boldPattern = Regex("^\\*\\*(.+?)\\*\\*$")
        val match = boldPattern.find(line)

        if (match != null) {
            // 返回加粗标记内的文本内容
            return match.groupValues[1].trim().takeUnless { it.isBlank() }
        }
    }

    return null
}

fun preprocessMarkdownForRender(content: String): String {
    val bracketBalanced = closeTrailingUnmatchedDisplayBracketMath(content)
    val codeBlocks = codeBlockRegex.findAll(bracketBalanced).map { it.range }.toList()

    fun isInCodeBlock(position: Int): Boolean {
        return codeBlocks.any { range -> position in range }
    }

    var result = inlineLatexRegex.replace(bracketBalanced) { match ->
        if (isInCodeBlock(match.range.first)) {
            match.value
        } else {
            "$" + match.groupValues[1] + "$"
        }
    }

    result = blockLatexRegex.replace(result) { match ->
        if (isInCodeBlock(match.range.first)) {
            match.value
        } else {
            "$$" + match.groupValues[1] + "$$"
        }
    }

    return closeTrailingUnmatchedDollarMathBlock(result)
}

private fun closeTrailingUnmatchedDisplayBracketMath(content: String): String {
    val codeBlocks = codeBlockRegex.findAll(content).map { it.range }.toList()

    fun isInCodeBlock(position: Int): Boolean {
        return codeBlocks.any { range -> position in range }
    }

    val openCount = displayBracketOpenRegex.findAll(content).count { !isInCodeBlock(it.range.first) }
    val closeCount = displayBracketCloseRegex.findAll(content).count { !isInCodeBlock(it.range.first) }
    if (openCount <= closeCount) return content

    return buildString {
        append(content.trimEnd())
        append('\n')
        append("\\]")
    }
}

private fun closeTrailingUnmatchedDollarMathBlock(content: String): String {
    val codeBlocks = codeBlockRegex.findAll(content).map { it.range }.toList()
    val fenceLines = mutableListOf<Int>()
    var lineStart = 0

    while (lineStart <= content.lastIndex) {
        val lineEndExclusive = content.indexOf('\n', lineStart).let { if (it == -1) content.length else it }
        val lineText = content.substring(lineStart, lineEndExclusive)
        if (lineText.trim() == "$$" && codeBlocks.none { range -> lineStart in range }) {
            fenceLines += lineStart
        }
        if (lineEndExclusive == content.length) break
        lineStart = lineEndExclusive + 1
    }

    if (fenceLines.size % 2 == 0) return content

    return buildString {
        append(content.trimEnd())
        append('\n')
        append("$$")
    }
}
