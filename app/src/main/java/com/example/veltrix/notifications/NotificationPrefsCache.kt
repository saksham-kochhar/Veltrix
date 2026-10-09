package com.example.veltrix.notifications

/**
 * In-memory cache for defense-in-depth when displaying local/push notifications.
 */
object NotificationPrefsCache {
    @Volatile
    var prefs: NotificationPreferences = NotificationPreferences()

    fun update(preferences: NotificationPreferences) {
        prefs = preferences
    }

    fun pushAllowed(categoryKey: String): Boolean {
        val p = prefs
        if (!p.channels.pushEnabled) return false
        return when (categoryKey) {
            "chat_updates" -> p.categories.chatUpdates
            "plan_updates" -> p.categories.planUpdates
            "promotional" -> p.categories.promotional
            "usage_alerts" -> p.categories.usageAlerts
            "security_alerts" -> p.categories.securityAlerts
            "offline_downloads" -> p.categories.offlineDownloads
            else -> false
        }
    }
}
