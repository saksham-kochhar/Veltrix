package com.example.veltrix

import org.json.JSONObject

data class WalletSnapshot(
    val creditsUsed: Long = 0,
    val creditsLimit: Long = 150_000,
    val creditsRemaining: Long = 150_000,
    val creditsUsedToday: Long = 0,
    val creditsLimitDay: Long = 5_000,
    val creditsUsed12h: Long = 0,
    val creditsLimit12h: Long = 2_500,
    val freeOnlyMode: Boolean = false,
    val tier: String = "free",
) {
    companion object {
        fun fromJson(obj: JSONObject): WalletSnapshot = WalletSnapshot(
            creditsUsed = obj.optLong("credits_used", 0),
            creditsLimit = obj.optLong("credits_limit", 150_000),
            creditsRemaining = obj.optLong("credits_remaining", 0),
            creditsUsedToday = obj.optLong("credits_used_today", 0),
            creditsLimitDay = obj.optLong("credits_limit_day", 5_000),
            creditsUsed12h = obj.optLong("credits_used_12h", 0),
            creditsLimit12h = obj.optLong("credits_limit_12h", 2_500),
            freeOnlyMode = obj.optBoolean("free_only_mode", false),
            tier = obj.optString("tier", "free"),
        )
    }
}
