package com.example.veltrix.Screen

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Lightweight markdown → AnnotatedString for chat bubbles.
 * Supports **bold**, *italic*, _italic_, `code`, and ## headings (as bold).
 */
fun parseChatMarkdown(
    raw: String,
    textColor: Color,
    codeBackgroundHint: Color = Color(0xFFE8E8F0)
): AnnotatedString {
    val normalized = raw
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        // Turn markdown bullets into a clearer glyph
        .replace(Regex("""(?m)^\s*[-*]\s+"""), "• ")

    return buildAnnotatedString {
        withStyle(SpanStyle(color = textColor)) {
            var i = 0
            while (i < normalized.length) {
                when {
                    // Headings: # ## ### at line start → bold rest of line
                    normalized.startsWith("#", i) &&
                        (i == 0 || normalized[i - 1] == '\n') -> {
                        var level = 0
                        while (i + level < normalized.length && normalized[i + level] == '#') level++
                        if (level in 1..6 &&
                            i + level < normalized.length &&
                            normalized[i + level] == ' '
                        ) {
                            i += level + 1
                            val end = normalized.indexOf('\n', i).let { if (it < 0) normalized.length else it }
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = textColor)) {
                                append(normalized.substring(i, end))
                            }
                            i = end
                            continue
                        }
                    }

                    // Bold **text**
                    normalized.startsWith("**", i) -> {
                        val end = normalized.indexOf("**", i + 2)
                        if (end > i + 2) {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = textColor)) {
                                append(normalized.substring(i + 2, end))
                            }
                            i = end + 2
                            continue
                        }
                    }

                    // Inline code `text`
                    normalized.startsWith("`", i) && !normalized.startsWith("```", i) -> {
                        val end = normalized.indexOf('`', i + 1)
                        if (end > i + 1) {
                            withStyle(
                                SpanStyle(
                                    fontFamily = FontFamily.Monospace,
                                    background = codeBackgroundHint.copy(alpha = 0.35f),
                                    color = textColor
                                )
                            ) {
                                append(normalized.substring(i + 1, end))
                            }
                            i = end + 1
                            continue
                        }
                    }

                    // Italic *text* (single asterisk, not **)
                    normalized.startsWith("*", i) && !normalized.startsWith("**", i) -> {
                        val end = findClosingSingleMarker(normalized, i, '*')
                        if (end > i + 1) {
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = textColor)) {
                                append(normalized.substring(i + 1, end))
                            }
                            i = end + 1
                            continue
                        }
                    }

                    // Italic _text_
                    normalized.startsWith("_", i) &&
                        (i == 0 || !normalized[i - 1].isLetterOrDigit()) -> {
                        val end = findClosingSingleMarker(normalized, i, '_')
                        if (end > i + 1) {
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = textColor)) {
                                append(normalized.substring(i + 1, end))
                            }
                            i = end + 1
                            continue
                        }
                    }
                }

                append(normalized[i])
                i++
            }
        }
    }
}

private fun findClosingSingleMarker(text: String, start: Int, marker: Char): Int {
    var j = start + 1
    while (j < text.length) {
        if (text[j] == marker) {
            // Avoid matching ** style doubles
            if (j + 1 < text.length && text[j + 1] == marker) {
                j += 2
                continue
            }
            return j
        }
        if (text[j] == '\n') return -1
        j++
    }
    return -1
}
