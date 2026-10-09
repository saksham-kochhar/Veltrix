package com.example.veltrix.security

import com.google.firebase.Timestamp

data class DeviceSession(
    val id: String,
    val platform: String = "",
    val deviceLabel: String = "",
    val appVersion: String = "",
    val createdAt: Timestamp? = null,
    val lastActiveAt: Timestamp? = null,
    val approxLocation: ApproxLocation? = null,
    val status: String = "active"
) {
    val isActive: Boolean get() = status == "active"
}

data class ApproxLocation(
    val city: String? = null,
    val country: String? = null,
    val ip: String? = null
) {
    fun displayLabel(): String {
        val parts = listOfNotNull(city?.takeIf { it.isNotBlank() }, country?.takeIf { it.isNotBlank() })
        return if (parts.isEmpty()) "Unknown location" else parts.joinToString(", ")
    }
}

data class LoginEvent(
    val id: String,
    val timestamp: Timestamp? = null,
    val success: Boolean = true,
    val method: String = "",
    val deviceLabel: String? = null,
    val approxLocation: ApproxLocation? = null,
    val failureReason: String? = null
)

data class SecurityEvent(
    val id: String,
    val type: String = "",
    val summary: String = "",
    val timestamp: Timestamp? = null
)

data class LinkedAuthProvider(
    val providerId: String,
    val email: String?
)
