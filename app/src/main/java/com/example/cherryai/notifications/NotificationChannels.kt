package com.example.cherryai.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val CHAT = "cherryai_chat"
    const val PLAN = "cherryai_plan"
    const val PROMO = "cherryai_promo"
    const val USAGE = "cherryai_usage"
    const val SECURITY = "cherryai_security"
    const val OFFLINE = "cherryai_offline"

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
