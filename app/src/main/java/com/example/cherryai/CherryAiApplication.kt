package com.example.cherryai

import android.app.Application
import com.example.cherryai.notifications.NotificationChannels

class CherryAiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.createAll(this)
    }
}
