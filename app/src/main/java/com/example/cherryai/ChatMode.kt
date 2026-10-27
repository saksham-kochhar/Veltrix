package com.example.cherryai

enum class ChatMode(val displayName: String) {
    Normal("Normal"),
    Brainstorm("Brainstorm"),
    Learn("Learn"),
    Coding("Coding");

    companion object {
        fun fromDisplay(name: String): ChatMode =
            entries.find { it.displayName.equals(name, ignoreCase = true) } ?: Normal
    }
}
