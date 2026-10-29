package com.example.cherryai.Screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.annotator.annotatorSettings
import com.mikepenz.markdown.annotator.buildMarkdownAnnotatedString
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.material.MarkdownBasicText
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.rememberMarkdownState
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import com.example.cherryai.ui.theme.cherryUiColors
import org.intellij.markdown.flavours.gfm.GFMTokenTypes

private fun normalizeChatMarkdown(raw: String): String =
    raw.trim().replace("\r\n", "\n").replace("\r", "\n")

@Composable
fun AssistantMarkdownContent(
    markdown: String,
    textColor: Color,
    linkColor: Color,
    baseStyle: TextStyle,
    codeBackgroundHint: Color = Color(0xFFE0E0EA)
) {
    val normalized = normalizeChatMarkdown(markdown)
    val markdownState = rememberMarkdownState(content = normalized)

    val bodyStyle = baseStyle.merge(
        TextStyle(
            color = textColor,
            platformStyle = PlatformTextStyle(includeFontPadding = false)
        )
    )

    val bubbleColorScheme = MaterialTheme.colorScheme.copy(primary = linkColor)
    val ui = cherryUiColors()
    val tableBorderColor = ui.tableBorder
    val tableBackground = Color.Transparent

    MaterialTheme(colorScheme = bubbleColorScheme) {
        Markdown(
            markdownState = markdownState,
            modifier = Modifier.fillMaxWidth(),
            colors = markdownColor(
                text = textColor,
                codeBackground = codeBackgroundHint.copy(alpha = 0.45f),
                tableBackground = Color.Transparent
            ),
            typography = markdownTypography(
                text = bodyStyle,
                paragraph = bodyStyle,
                table = bodyStyle,
                h1 = bodyStyle.merge(TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)),
                h2 = bodyStyle.merge(TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)),
                h3 = bodyStyle.merge(TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp)),
                h4 = bodyStyle.merge(TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)),
                h5 = bodyStyle,
                h6 = bodyStyle,
                code = bodyStyle.merge(
                    TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                ),
                quote = bodyStyle.merge(TextStyle(color = textColor.copy(alpha = 0.88f))),
                ordered = bodyStyle,
                bullet = bodyStyle,
                list = bodyStyle
            ),
            components = markdownComponents(
                table = { model ->
                    ChatWrappedMarkdownTable(
                        content = model.content,
                        node = model.node,
                        cellStyle = model.typography.table,
                        borderColor = tableBorderColor,
                        backgroundColor = tableBackground
                    )
                }
            )
        )
    }
}

@Composable
private fun ChatWrappedMarkdownTable(
    content: String,
    node: ASTNode,
    cellStyle: TextStyle,
    borderColor: Color,
    backgroundColor: Color
) {
    val rows = node.children.filter {
        it.type == GFMElementTypes.HEADER || it.type == GFMElementTypes.ROW
    }
    if (rows.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
    ) {
        rows.forEachIndexed { index, rowNode ->
            val isHeader = rowNode.type == GFMElementTypes.HEADER
            ChatMarkdownTableRow(
                content = content,
                rowNode = rowNode,
                cellStyle = if (isHeader) {
                    cellStyle.merge(TextStyle(fontWeight = FontWeight.SemiBold))
                } else {
                    cellStyle
                }
            )
            if (index < rows.lastIndex) {
                HorizontalDivider(color = borderColor, thickness = 1.dp)
            }
        }
    }
}

@Composable
private fun ChatMarkdownTableRow(
    content: String,
    rowNode: ASTNode,
    cellStyle: TextStyle
) {
    val cells = rowNode.children.filter { it.type == GFMTokenTypes.CELL }
    if (cells.isEmpty()) return

    Row(modifier = Modifier.fillMaxWidth()) {
        cells.forEach { cell ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                val annotated = content.buildMarkdownAnnotatedString(
                    textNode = cell,
                    style = cellStyle,
                    annotatorSettings = annotatorSettings()
                )
                MarkdownBasicText(
                    text = annotated,
                    style = cellStyle,
                    modifier = Modifier.fillMaxWidth(),
                    softWrap = true,
                    maxLines = Int.MAX_VALUE,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}
