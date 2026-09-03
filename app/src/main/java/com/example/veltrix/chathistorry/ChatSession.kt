package com.example.veltrix.chathistorry

import com.example.veltrix.Response

data class ChatSession(
    val id: String = "",
    val title: String = "",
    val summary: String = "",
    val lastMessage: String = "",
    val updatedAt: Long = 0L,
    val titleRefined: Boolean = false,
    val messages: List<Response> = emptyList()
)

/** Drawer row — full messages only when loaded from local or Firestore. */
data class ChatSessionSummary(
    val id: String = "",
    val title: String = "",
    val summary: String = "",
    val lastMessage: String = "",
    val updatedAt: Long = 0L,
    val cachedLocally: Boolean = false
)

fun ChatSession.toSummary(cachedLocally: Boolean = true) = ChatSessionSummary(
    id = id,
    title = title,
    summary = summary,
    lastMessage = lastMessage,
    updatedAt = updatedAt,
    cachedLocally = cachedLocally
)
