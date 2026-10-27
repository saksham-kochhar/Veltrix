package com.example.cherryai

import android.content.Context
import java.time.LocalDate
import java.time.ZoneOffset

data class UsageAlertPrompt(
    val scope: String,
    val level: Int,
    val message: String,
    val ackKey: String,
)

object UsageAlertStore {
    private const val PREFS = "cherryai_usage_alerts"

    fun nextAlert(context: Context, wallet: WalletSnapshot): UsageAlertPrompt? {
        val periodEnd = wallet.periodEnd.ifBlank { "unknown" }
        val today = LocalDate.now(ZoneOffset.UTC).toString()
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        if (wallet.dailyAlertLevel > 0 && wallet.dailyAlertMessage.isNotBlank()) {
            val key = "$periodEnd|$today|daily|${wallet.dailyAlertLevel}"
            if (prefs.getInt(key, 0) < wallet.dailyAlertLevel) {
                return UsageAlertPrompt("daily", wallet.dailyAlertLevel, wallet.dailyAlertMessage, key)
            }
        }
        if (wallet.monthlyAlertLevel > 0 && wallet.monthlyAlertMessage.isNotBlank()) {
            val key = "$periodEnd|monthly|${wallet.monthlyAlertLevel}"
            if (prefs.getInt(key, 0) < wallet.monthlyAlertLevel) {
                return UsageAlertPrompt("monthly", wallet.monthlyAlertLevel, wallet.monthlyAlertMessage, key)
            }
        }
        return null
    }

    fun acknowledge(context: Context, prompt: UsageAlertPrompt) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(prompt.ackKey, prompt.level)
            .apply()
    }
}
