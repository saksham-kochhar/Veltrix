package com.example.veltrix.chathistorry

import com.example.veltrix.Response

const val COMPACTION_WINDOW = 15
const val CONTEXT_SUMMARY_MAX_WORDS = 180
const val PROFILE_SUMMARY_MAX_WORDS = 120
const val PROFILE_CHAT_BATCH = 15

const val COMPACT_META_HINT =
    "Max $CONTEXT_SUMMARY_MAX_WORDS words. Preserve decisions, preferences, constraints, unresolved tasks."

fun unsummarizedTail(
    messages: List<Response>,
    summarizedUntilMessage: Int
): List<Response> {
    if (summarizedUntilMessage <= 0) return messages
    if (summarizedUntilMessage >= messages.size) return emptyList()
    return messages.subList(summarizedUntilMessage, messages.size)
}

fun compactionChunk(
    messages: List<Response>,
    summarizedUntilMessage: Int,
    window: Int = COMPACTION_WINDOW
): List<Response> {
    if (window <= 0) return emptyList()
    val start = summarizedUntilMessage.coerceAtLeast(0)
    val end = (start + window).coerceAtMost(messages.size)
    if (start >= end) return emptyList()
    return messages.subList(start, end)
}

fun shouldCompact(
    messagesSinceSummary: Int,
    window: Int = COMPACTION_WINDOW
): Boolean = messagesSinceSummary >= window

fun shouldRefreshProfile(
    summarizedChatCount: Int,
    batch: Int = PROFILE_CHAT_BATCH
): Boolean = summarizedChatCount > 0 && summarizedChatCount % batch == 0

fun clampWords(text: String, maxWords: Int): String {
    val words = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.size <= maxWords) return words.joinToString(" ")
    return words.take(maxWords).joinToString(" ")
}
