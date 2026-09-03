package com.example.veltrix.chathistorry

import com.example.veltrix.Response

fun countUserMessages(messages: List<Response>): Int =
    messages.count { it.Role == "User" }

fun messagesUpToNthUser(messages: List<Response>, n: Int): List<Response> {
    if (n <= 0) return emptyList()
    var userCount = 0
    val result = mutableListOf<Response>()
    for (msg in messages) {
        result.add(msg)
        if (msg.Role == "User") {
            userCount++
            if (userCount >= n) break
        }
    }
    return result
}

fun firstUserMessage(messages: List<Response>): String =
    messages.firstOrNull { it.Role == "User" }?.message?.trim().orEmpty()

fun needsInitialTitle(title: String): Boolean =
    title.isBlank() || title.equals("New chat", ignoreCase = true)

fun conversationText(messages: List<Response>): String =
    messages.joinToString("\n") { msg ->
        val role = if (msg.Role == "User") "User" else "Assistant"
        "$role: ${msg.message.trim()}"
    }

fun buildInitialTitlePrompt(firstMessage: String): String =
    "Opening user message:\n$firstMessage"

fun buildRefinedTitlePrompt(messages: List<Response>): String =
    "Conversation:\n${conversationText(messages)}"

fun buildSummaryPrompt(messages: List<Response>): String =
    "Conversation:\n${conversationText(messages)}"

const val TITLE_INSTRUCTION =
    "Reply with ONLY a 3-6 word topic title. No quotes, no punctuation at the end, no explanation."

const val SUMMARY_INSTRUCTION =
    "Summarize this conversation in ONE short sentence, max 12 words. No quotes, no explanation."

private val GREETING_PREFIXES = listOf(
    "hello", "hi", "hey", "good morning", "good afternoon", "good evening"
)

fun heuristicTitle(firstMessage: String): String {
    var text = firstMessage.trim()
    if (text.isEmpty()) return "New chat"

    val lower = text.lowercase()
    for (greeting in GREETING_PREFIXES) {
        if (lower.startsWith(greeting)) {
            text = text.drop(greeting.length).trim().trimStart(',', '.', '!', '?', ' ')
            break
        }
    }

    if (text.isEmpty()) {
        return when {
            lower.contains("code") -> "Coding help"
            lower.contains("learn") -> "Learning chat"
            else -> "New conversation"
        }
    }

    val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
    val short = words.take(6).joinToString(" ")
    return sanitizeTitle(short.ifBlank { "New chat" })
}
fun sanitizeTitle(raw: String): String {
    var text = raw.trim().trim('"', '\'', '“', '”').trim()
    if (text.endsWith(".")) text = text.dropLast(1).trim()
    if (text.length <= 60) return text
    return text.take(57).trimEnd() + "…"
}

fun sanitizeSummary(raw: String): String {
    var text = raw.trim().trim('"', '\'', '“', '”').trim()
    if (text.endsWith(".")) text = text.dropLast(1).trim()
    if (text.length <= 80) return text
    return text.take(77).trimEnd() + "…"
}
