package com.example.cherryai.ui.theme

import android.content.Context

enum class ThemeMode(val label: String, val storageValue: String) {
    LIGHT("Light", "light"),
    DARK("Dark", "dark"),
    SYSTEM("System default", "system");

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}

object ThemePreferences {
    private const val PREFS = "app_theme_v1"
    private const val KEY_MODE = "theme_mode"

    fun getMode(context: Context): ThemeMode {
        val raw = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_MODE, ThemeMode.SYSTEM.storageValue)
        return ThemeMode.fromStorage(raw)
    }

    fun setMode(context: Context, mode: ThemeMode) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode.storageValue)
            .apply()
    }
}
