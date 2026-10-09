package com.example.veltrix.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val CHAT = "veltrix_chat"
    const val PLAN = "veltrix_plan"
    const val PROMO = "veltrix_promo"
    const val USAGE = "veltrix_usage"
    const val SECURITY = "veltrix_security"
    const val OFFLINE = "veltrix_offline"

    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channels = listOf(
            channel(CHAT, "Chat updates", NotificationManager.IMPORTANCE_DEFAULT),
            channel(PLAN, "Plan & subscription", NotificationManager.IMPORTANCE_HIGH),
            channel(PROMO, "Offers & tips", NotificationManager.IMPORTANCE_LOW),
            channel(USAGE, "Usage & limits", NotificationManager.IMPORTANCE_DEFAULT),
            channel(SECURITY, "Security alerts", NotificationManager.IMPORTANCE_HIGH),
            channel(OFFLINE, "Offline model", NotificationManager.IMPORTANCE_DEFAULT),
        )
        manager.createNotificationChannels(channels)
    }

    private fun channel(id: String, name: String, importance: Int): NotificationChannel {
        return NotificationChannel(id, name, importance)
    }
}
