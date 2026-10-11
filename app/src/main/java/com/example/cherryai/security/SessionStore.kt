package com.example.cherryai.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

object SessionStore {
    private const val TAG = "SessionStore"
    private const val PREFS = "cherryai_security_session"
    private const val FALLBACK_PREFS = "cherryai_security_session_fallback"
    private const val KEY_SESSION_ID = "session_id"

    private fun encryptedPrefs(context: Context): SharedPreferences {
        val app = context.applicationContext
        val masterKey = MasterKey.Builder(app)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            app,
            PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun prefs(context: Context): SharedPreferences {
        val app = context.applicationContext
        return try {
            encryptedPrefs(app)
        } catch (first: Exception) {
            Log.w(TAG, "Encrypted prefs unavailable, using fallback store", first)
            try {
                app.deleteSharedPreferences(PREFS)
            } catch (_: Exception) {
                // ignore
            }
            try {
                encryptedPrefs(app)
            } catch (second: Exception) {
                Log.w(TAG, "Encrypted prefs retry failed, using plain prefs", second)
                app.getSharedPreferences(FALLBACK_PREFS, Context.MODE_PRIVATE)
            }
        }
    }

    fun getOrCreateSessionId(context: Context): String {
        return try {
            val p = prefs(context.applicationContext)
            val existing = p.getString(KEY_SESSION_ID, null)
            if (!existing.isNullOrBlank()) return existing
            val created = UUID.randomUUID().toString().replace("-", "")
            p.edit().putString(KEY_SESSION_ID, created).apply()
            created
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist session id", e)
            UUID.randomUUID().toString().replace("-", "")
        }
    }

    fun clearSessionId(context: Context) {
        runCatching {
            prefs(context.applicationContext).edit().remove(KEY_SESSION_ID).apply()
        }
    }
}
