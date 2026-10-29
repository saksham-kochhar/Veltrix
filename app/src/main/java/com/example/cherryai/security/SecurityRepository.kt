package com.example.cherryai.security

import android.content.Context
import android.os.Build
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SecurityRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val backendBase: String = BACKEND_BASE
) {
    companion object {
        const val BACKEND_BASE = "https://veltrix-backend-nmvy.onrender.com"
    }

    private suspend fun idToken(forceRefresh: Boolean = false): String =
        auth.currentUser?.getIdToken(forceRefresh)?.await()?.token
            ?: throw IllegalStateException("Not signed in")

    fun deviceLabel(): String = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

    fun appVersion(context: Context): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    }.getOrDefault("")

    private suspend fun authorizedJsonRequest(
        method: String,
        path: String,
        body: JSONObject? = null,
        forceRefreshToken: Boolean = false,
    ): String = withContext(Dispatchers.IO) {
        val connection = (URL("$backendBase$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Authorization", "Bearer ${idToken(forceRefreshToken)}")
            setRequestProperty("Content-Type", "application/json")
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
            throw IllegalStateException("Security API $code: $text")
        }
        text
    }

    private suspend fun publicJsonGet(path: String): String = withContext(Dispatchers.IO) {
        val connection = (URL("$backendBase$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            doInput = true
        }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.readText().orEmpty()
        if (code !in 200..299) {
            throw IllegalStateException("Security API $code: $text")
        }
        text
    }

    private suspend fun publicJsonPost(path: String, body: JSONObject): String = withContext(Dispatchers.IO) {
        val connection = (URL("$backendBase$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            doInput = true
            outputStream.use { it.write(body.toString().toByteArray()) }
        }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.readText().orEmpty()
        if (code !in 200..299) {
            throw IllegalStateException("Security API $code: $text")
        }
        text
    }

    suspend fun loginWithPassword(email: String, password: String): PasswordLoginResult =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("email", email.trim())
                .put("password", password)
            val connection = (URL("$backendBase/v1/security/login/password").openConnection()
                as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                doInput = true
                outputStream.use { it.write(body.toString().toByteArray()) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.readText().orEmpty()
            if (code !in 200..299) {
                val message = parseFastApiErrorMessage(text)
                val locked = text.contains("\"locked\":true") || text.contains("\"locked\": true")
                throw PasswordLoginException(message, locked)
            }
            val json = JSONObject(text)
            PasswordLoginResult(
                customToken = json.getString("custom_token"),
                email = json.optString("email", email.trim()),
                emailVerified = json.optBoolean("email_verified", false),
            )
        }

    private fun parseFastApiErrorMessage(text: String): String {
        return runCatching {
            val root = JSONObject(text)
            when (val detail = root.opt("detail")) {
                is String -> detail
                is JSONObject -> detail.optString("message").takeIf { it.isNotBlank() }
                    ?: "Invalid email or password"
                else -> "Invalid email or password"
            }
        }.getOrDefault("Invalid email or password")
    }

    suspend fun registerSessionAfterLogin(context: Context, method: String = "password") {
        val sessionId = SessionStore.getOrCreateSessionId(context)
        val body = JSONObject()
            .put("session_id", sessionId)
            .put("platform", "android")
            .put("device_label", deviceLabel())
            .put("app_version", appVersion(context))
            .put("method", method)
        authorizedJsonRequest("POST", "/v1/security/sessions/register", body)
    }

    suspend fun touchSessionActivity(context: Context) {
        val sessionId = SessionStore.getOrCreateSessionId(context)
        val body = JSONObject()
            .put("session_id", sessionId)
            .put("platform", "android")
            .put("device_label", deviceLabel())
        authorizedJsonRequest("POST", "/v1/security/sessions/touch", body)
    }

    suspend fun isCurrentSessionRevoked(context: Context): Boolean {
        return runCatching {
            val sessionId = SessionStore.getOrCreateSessionId(context)
            val response = authorizedJsonRequest(
                "GET",
                "/v1/security/sessions/status?session_id=$sessionId"
            )
            if (!JSONObject(response).optBoolean("revoked", false)) {
                return@runCatching false
            }
            // Older backend builds treated unknown session ids as revoked; confirm the doc exists.
            fetchSessions().any { it.id == sessionId }
        }.getOrDefault(false)
    }

    /**
     * Registers the device session if needed, keeps activity fresh, and reports whether
     * this device was explicitly revoked server-side (sign-out-all, remote revoke, etc.).
     */
    suspend fun syncSecuritySession(context: Context, method: String = "password"): Boolean {
        val app = context.applicationContext
        if (isCurrentSessionRevoked(app)) {
            return true
        }
        registerSessionAfterLogin(app, method)
        touchSessionActivity(app)
        return false
    }

    suspend fun fetchSessions(): List<DeviceSession> {
        val response = authorizedJsonRequest("GET", "/v1/security/sessions")
        val array = JSONArray(response)
        return (0 until array.length()).mapNotNull { i ->
            array.optJSONObject(i)?.toDeviceSession()
        }
    }

    suspend fun fetchLoginEvents(limit: Int = 50): List<LoginEvent> {
        val response = authorizedJsonRequest("GET", "/v1/security/login-events?limit=$limit")
        val array = JSONArray(response)
        return (0 until array.length()).mapNotNull { i ->
            array.optJSONObject(i)?.toLoginEvent()
        }
    }

    suspend fun fetchSecurityEvents(limit: Int = 30): List<SecurityEvent> {
        val response = authorizedJsonRequest("GET", "/v1/security/events?limit=$limit")
        val array = JSONArray(response)
        return (0 until array.length()).mapNotNull { i ->
            array.optJSONObject(i)?.toSecurityEvent()
        }
    }

    suspend fun revokeSession(sessionId: String) {
        authorizedJsonRequest("POST", "/v1/security/sessions/$sessionId/revoke")
    }

    suspend fun signOutAllDevices(
        keepCurrentSessionId: String?,
        forceRefreshToken: Boolean = false,
    ) {
        val body = JSONObject().put(
            "keep_session_id",
            keepCurrentSessionId ?: JSONObject.NULL
        )
        authorizedJsonRequest(
            "POST",
            "/v1/security/sign-out-all",
            body,
            forceRefreshToken,
        )
    }

    suspend fun logSecurityEvent(
        type: String,
        summary: String,
        forceRefreshToken: Boolean = false,
    ) {
        val body = JSONObject()
            .put("type", type)
            .put("summary", summary)
            .put("metadata", JSONObject())
        authorizedJsonRequest(
            "POST",
            "/v1/security/events",
            body,
            forceRefreshToken,
        )
    }

    suspend fun logFailedLogin(email: String, failureReason: String) {
        runCatching {
            val body = JSONObject()
                .put("email", email.trim())
                .put("method", "password")
                .put("failure_reason", failureReason)
            publicJsonPost("/v1/security/login-events/failed", body)
        }
    }

    fun linkedProviders(): List<LinkedAuthProvider> {
        val user = auth.currentUser ?: return emptyList()
        return user.providerData.mapNotNull { info ->
            val id = info.providerId ?: return@mapNotNull null
            if (id == "firebase") return@mapNotNull null
            LinkedAuthProvider(providerId = id, email = info.email)
        }
    }

    fun hasPasswordProvider(): Boolean =
        auth.currentUser?.providerData?.any {
            it.providerId == com.google.firebase.auth.EmailAuthProvider.PROVIDER_ID
        } == true

    fun hasGoogleProvider(): Boolean =
        auth.currentUser?.providerData?.any {
            it.providerId == GoogleAuthProvider.PROVIDER_ID
        } == true

    fun primaryAccountEmail(): String? = auth.currentUser?.email?.takeIf { it.isNotBlank() }

    private fun JSONObject.toDeviceSession(): DeviceSession {
        return DeviceSession(
            id = optString("id"),
            platform = optString("platform"),
            deviceLabel = optString("device_label"),
            appVersion = optString("app_version"),
            createdAt = optLongOrNull("created_at")?.toTimestamp(),
            lastActiveAt = optLongOrNull("last_active_at")?.toTimestamp(),
            approxLocation = optJSONObject("approx_location")?.toApproxLocation(),
            status = optString("status", "active")
        )
    }

    private fun JSONObject.toLoginGate(): LoginGateStatus {
        return LoginGateStatus(
            allowed = optBoolean("allowed", true),
            attemptsRemaining = optInt("attempts_remaining", 3),
            retryAfterSeconds = optInt("retry_after_seconds", 0),
            locked = optBoolean("locked", false),
            message = optString("message").takeIf { it.isNotBlank() },
        )
    }

    private fun JSONObject.toLoginEvent(): LoginEvent {
        return LoginEvent(
            id = optString("id"),
            timestamp = optLongOrNull("timestamp")?.toTimestamp(),
            success = optBoolean("success", true),
            method = optString("method"),
            deviceLabel = optString("device_label").takeIf { it.isNotBlank() },
            approxLocation = optJSONObject("approx_location")?.toApproxLocation(),
            failureReason = optString("failure_reason").takeIf { it.isNotBlank() }
        )
    }

    private fun JSONObject.toSecurityEvent(): SecurityEvent {
        return SecurityEvent(
            id = optString("id"),
            type = optString("type"),
            summary = optString("summary"),
            timestamp = optLongOrNull("timestamp")?.toTimestamp()
        )
    }

    private fun JSONObject.toApproxLocation(): ApproxLocation {
        return ApproxLocation(
            city = optString("city").takeIf { it.isNotBlank() },
            country = optString("country").takeIf { it.isNotBlank() },
            ip = optString("ip").takeIf { it.isNotBlank() }
        )
    }

    private fun JSONObject.optLongOrNull(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        return optLong(key)
    }

    private fun Long.toTimestamp(): Timestamp = Timestamp(this, 0)
}
