package me.rerere.rikkahub.ui.components.richtext

import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import me.rerere.rikkahub.utils.preprocessMarkdownForRender

class MarkdownStreamingMathTest {
    private val parser = MarkdownParser(
        GFMFlavourDescriptor(
            makeHttpsAutoLinks = true,
            useSafeLinks = true,
        )
    )

    @Test
    fun `unclosed block math keeps plus and minus lines inside math block during streaming`() {
        val content = """
            $$
            A = \begin{bmatrix}
            1 & 2 \\
            +3 & 4 \\
            -5 & 6
        """.trimIndent()

        val preprocessed = preprocessMarkdownForRender(content)
        val tree = parser.buildMarkdownTreeFromString(preprocessed)

        assertEquals(
            "streaming parse should keep the trailing math block closed for rendering",
            1,
            countNodesOfType(tree, GFMElementTypes.BLOCK_MATH)
        )
        assertFalse(
            "unfinished math content must not be reinterpreted as a markdown list",
            containsNodeType(tree, MarkdownElementTypes.UNORDERED_LIST)
        )
    }

    @Test
    fun `closed block math is left unchanged`() {
        val content = """
            $$
            x + y
            $$
        """.trimIndent()

        assertEquals(content, preprocessMarkdownForRender(content))
    }

    private fun countNodesOfType(node: ASTNode, type: org.intellij.markdown.IElementType): Int {
        var count = if (node.type == type) 1 else 0
        node.children.forEach { child ->
            count += countNodesOfType(child, type)
        }
        return count
    }

    private fun containsNodeType(node: ASTNode, type: org.intellij.markdown.IElementType): Boolean {
        if (node.type == type) return true
        return node.children.any { child -> containsNodeType(child, type) }
    }
}
