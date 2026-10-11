package com.example.cherryai.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CherryAiMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        PushTokenRegistrar.onNewToken(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data.isEmpty()) return
        LocalNotificationHelper.showFromPushData(applicationContext, data)
    }
}
