package com.example.veltrix.Screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownTable
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.rememberMarkdownState

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
                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                ) {
                    MarkdownTable(
                        content = model.content,
                        node = model.node,
                        style = model.typography.table
                    )
                }
            }
        )
    )
    }
}
