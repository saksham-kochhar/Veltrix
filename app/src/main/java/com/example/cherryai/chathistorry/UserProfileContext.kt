package com.example.cherryai.chathistorry

import com.example.cherryai.UserProfile

/** Stable facts from the user document — not derived from chat summarization. */
fun buildStructuredProfileContext(profile: UserProfile?): String {
    if (profile == null) return ""
    val parts = mutableListOf<String>()
    val name = listOf(profile.firstname, profile.lastname)
        .filter { it.isNotBlank() }
        .joinToString(" ")
    if (name.isNotBlank()) parts.add("Name: $name")
    if (profile.age > 0) parts.add("Age: ${profile.age}")
    if (profile.gender.isNotBlank()) parts.add("Gender: ${profile.gender}")
    return parts.joinToString(". ")
}

fun profileContextForNewChat(profile: UserProfile?, profileSummary: String): String {
    val structured = buildStructuredProfileContext(profile)
    val summary = profileSummary.trim()
    val combined = when {
        structured.isBlank() && summary.isBlank() -> ""
        structured.isBlank() -> summary
        summary.isBlank() -> structured
        else -> "$structured\n\nLong-term context:\n$summary"
    }
    return clampWords(combined, PROFILE_SUMMARY_MAX_WORDS)
}
