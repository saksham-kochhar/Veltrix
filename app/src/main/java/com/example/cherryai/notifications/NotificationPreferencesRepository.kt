package com.example.cherryai.notifications

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class NotificationPreferencesRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val backendBase: String = BACKEND_BASE,
) {
    companion object {
        const val BACKEND_BASE = com.example.cherryai.security.SecurityRepository.BACKEND_BASE
    }

    private suspend fun idToken(): String =
        auth.currentUser?.getIdToken(false)?.await()?.token
            ?: throw IllegalStateException("Not signed in")

    private suspend fun authorizedJsonRequest(
        method: String,
        path: String,
        body: JSONObject? = null,
    ): String = withContext(Dispatchers.IO) {
        val connection = (URL("$backendBase$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Authorization", "Bearer ${idToken()}")
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 8_000
            readTimeout = 12_000
            doInput = true
            if (body != null) {
                doOutput = true
                outputStream.use { it.write(body.toString().toByteArray()) }
            }
        }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.readText().orEmpty()
        if (code !in 200..299) {
            val hint = when (code) {
                404 -> " (notifications API not deployed — update the backend on Render)"
                else -> ""
            }
            throw IllegalStateException("Notifications API $code$hint: $text")
        }
        text
    }

    suspend fun fetchPreferences(): NotificationPreferences {
        val response = authorizedJsonRequest("GET", "/v1/notifications/preferences")
        return NotificationPreferences.fromJson(JSONObject(response))
    }

    suspend fun updatePreferences(patch: JSONObject): NotificationPreferences {
        val response = authorizedJsonRequest("PUT", "/v1/notifications/preferences", patch)
        return NotificationPreferences.fromJson(JSONObject(response))
    }

    suspend fun registerDevice(token: String, platform: String, appVersion: String) {
        val body = JSONObject()
            .put("token", token)
            .put("platform", platform)
            .put("app_version", appVersion)
        authorizedJsonRequest("POST", "/v1/notifications/devices/register", body)
    }

    suspend fun unregisterDevice(token: String) {
        val encoded = java.net.URLEncoder.encode(token, "UTF-8")
        authorizedJsonRequest("DELETE", "/v1/notifications/devices/$encoded")
    }
}
