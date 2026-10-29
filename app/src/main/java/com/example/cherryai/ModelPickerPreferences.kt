package com.example.cherryai

import android.content.Context

object ModelPickerPreferences {
    private const val PREFS = "model_picker_v1"
    private const val KEY_ONLY_INCLUDED = "only_included_models"

    fun onlyIncludedModels(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONLY_INCLUDED, false)

    fun setOnlyIncludedModels(context: Context, enabled: Boolean) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ONLY_INCLUDED, enabled)
            .apply()
    }
}
