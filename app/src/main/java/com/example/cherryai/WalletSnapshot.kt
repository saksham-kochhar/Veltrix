package com.example.cherryai

import org.json.JSONObject

data class WalletSnapshot(
    val creditsUsed: Long = 0,
    val creditsLimit: Long = 50_000,
    val creditsRemaining: Long = 50_000,
    val creditsRemainingMonth: Long = 50_000,
    val creditsRemainingDay: Long = 50_000,
    val creditsUsedToday: Long = 0,
    val creditsLimitDay: Long = 50_000,
    val creditsUsed12h: Long = 0,
    val creditsLimit12h: Long = 25_000,
    val freeOnlyMode: Boolean = false,
    val tier: String = "free",
    val monthlyUsedPercent: Int = 0,
    val monthlyRemainingPercent: Int = 100,
    val dailyUsedPercent: Int = 0,
    val dailyRemainingPercent: Int = 100,
    val dailyAlertLevel: Int = 0,
    val monthlyAlertLevel: Int = 0,
    val dailyAlertMessage: String = "",
    val monthlyAlertMessage: String = "",
    val periodEnd: String = "",
    val limitBlockReason: String = "none",
) {
    companion object {
        fun fromJson(obj: JSONObject): WalletSnapshot = WalletSnapshot(
            creditsUsed = obj.optLong("credits_used", 0),
            creditsLimit = obj.optLong("credits_limit", 50_000),
            creditsRemaining = obj.optLong("credits_remaining", 0),
            creditsRemainingMonth = obj.optLong(
                "credits_remaining_month",
                obj.optLong("credits_remaining", 0),
            ),
            creditsRemainingDay = obj.optLong("credits_remaining_day", 0),
            creditsUsedToday = obj.optLong("credits_used_today", 0),
            creditsLimitDay = obj.optLong("credits_limit_day", 50_000),
            creditsUsed12h = obj.optLong("credits_used_12h", 0),
            creditsLimit12h = obj.optLong("credits_limit_12h", 25_000),
            freeOnlyMode = obj.optBoolean("free_only_mode", false),
            tier = obj.optString("tier", "free"),
            monthlyUsedPercent = obj.optInt("monthly_used_percent", 0),
            monthlyRemainingPercent = obj.optInt("monthly_remaining_percent", 100),
            dailyUsedPercent = obj.optInt("daily_used_percent", 0),
            dailyRemainingPercent = obj.optInt("daily_remaining_percent", 100),
            dailyAlertLevel = obj.optInt("daily_alert_level", 0),
            monthlyAlertLevel = obj.optInt("monthly_alert_level", 0),
            dailyAlertMessage = obj.optString("daily_alert_message", ""),
            monthlyAlertMessage = obj.optString("monthly_alert_message", ""),
            periodEnd = obj.optString("period_end", ""),
            limitBlockReason = obj.optString("limit_block_reason", "none"),
        )
    }
}
