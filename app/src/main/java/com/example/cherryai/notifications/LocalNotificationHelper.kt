package com.example.cherryai.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.cherryai.MainActivity
import com.example.cherryai.R

object LocalNotificationHelper {
    fun showOfflineDownload(context: Context, success: Boolean) {
        if (!NotificationPrefsCache.pushAllowed("offline_downloads")) return
        if (!NotificationPrefsCache.prefs.channels.inAppEnabled) {
            // Still show system notification; in-app master is for in-app banners only.
        }
        val title = if (success) "Offline model ready" else "Offline download failed"
        val body = if (success) {
            "Your on-device model finished downloading."
        } else {
            "Could not download the offline model. Try again on Wi‑Fi."
        }
        show(
            context = context,
            channelId = NotificationChannels.OFFLINE,
            notificationId = 2001,
            title = title,
            body = body,
            route = "home",
        )
    }

    fun showFromPushData(context: Context, data: Map<String, String>) {
        val category = data["category"] ?: return
        if (!NotificationPrefsCache.pushAllowed(category)) return
        val channelId = data["channel_id"] ?: NotificationChannels.CHAT
        val title = data["title"] ?: "Cherry AI"
        val body = data["body"] ?: ""
        val route = data["route"] ?: "home"
        val sessionId = data["session_id"]
        show(
            context = context,
            channelId = channelId,
            notificationId = (data["event_id"] ?: category).hashCode(),
            title = title,
            body = body,
            route = route,
            sessionId = sessionId,
        )
    }

    private fun show(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String,
        route: String,
        sessionId: String? = null,
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_NOTIFICATION_ROUTE, route)
            sessionId?.let { putExtra(MainActivity.EXTRA_SESSION_ID, it) }
        }
        val pending = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, notification)
    }
}
