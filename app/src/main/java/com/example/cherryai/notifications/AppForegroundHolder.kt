package com.example.cherryai.notifications

object AppForegroundHolder {
    @Volatile
    var isForeground: Boolean = false

    fun appStateHeader(): String =
        if (isForeground) "foreground" else "background"
}
