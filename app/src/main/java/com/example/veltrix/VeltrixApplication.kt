package com.example.veltrix

import android.app.Application
import com.example.veltrix.notifications.NotificationChannels

class VeltrixApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.createAll(this)
    }
}
