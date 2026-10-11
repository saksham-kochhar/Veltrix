package com.example.cherryai.notifications

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object PushTokenStore {
    private const val PREFS = "cherryai_push_token"
    private const val KEY_TOKEN = "fcm_token"

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun save(context: Context, token: String) {
        prefs(context).edit().putString(KEY_TOKEN, token).apply()
    }

    fun get(context: Context): String? =
        prefs(context).getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_TOKEN).apply()
    }
}
