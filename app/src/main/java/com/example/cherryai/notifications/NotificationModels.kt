package com.example.cherryai.notifications

import org.json.JSONObject

data class NotificationChannelSettings(
    val pushEnabled: Boolean = true,
    val emailEnabled: Boolean = true,
    val inAppEnabled: Boolean = true,
)

data class NotificationCategories(
    val chatUpdates: Boolean = true,
    val planUpdates: Boolean = true,
    val promotional: Boolean = false,
    val usageAlerts: Boolean = true,
    val securityAlerts: Boolean = true,
    val offlineDownloads: Boolean = true,
)

data class NotificationPreferences(
    val channels: NotificationChannelSettings = NotificationChannelSettings(),
    val categories: NotificationCategories = NotificationCategories(),
    val serverEmailEnabled: Boolean = false,
) {
    fun securityAlertsSubtitle(): String {
        val pushOn = channels.pushEnabled && categories.securityAlerts
        val emailOn =
            serverEmailEnabled && channels.emailEnabled && categories.securityAlerts
        return when {
            pushOn && emailOn -> "Push and email alerts for important account changes."
            pushOn -> "Push alerts on; email alerts off."
            emailOn -> "Email alerts on; push alerts off."
            else -> "Push and email delivery off. You will still see alerts in Security."
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): NotificationPreferences {
            val channels = obj.optJSONObject("channels")
            val categories = obj.optJSONObject("categories")
            return NotificationPreferences(
                channels = NotificationChannelSettings(
                    pushEnabled = channels?.optBoolean("pushEnabled", true) ?: true,
                    emailEnabled = channels?.optBoolean("emailEnabled", true) ?: true,
                    inAppEnabled = channels?.optBoolean("inAppEnabled", true) ?: true,
                ),
                categories = NotificationCategories(
                    chatUpdates = categories?.optBoolean("chat_updates", true) ?: true,
                    planUpdates = categories?.optBoolean("plan_updates", true) ?: true,
                    promotional = categories?.optBoolean("promotional", false) ?: false,
                    usageAlerts = categories?.optBoolean("usage_alerts", true) ?: true,
                    securityAlerts = categories?.optBoolean("security_alerts", true) ?: true,
                    offlineDownloads = categories?.optBoolean("offline_downloads", true) ?: true,
                ),
                serverEmailEnabled = obj.optBoolean("serverEmailEnabled", false),
            )
        }
    }
}
