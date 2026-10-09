package com.example.veltrix.notifications

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object PushTokenRegistrar {
    suspend fun registerCurrentToken(context: Context) {
        if (FirebaseAuth.getInstance().currentUser == null) return
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
            ?: PushTokenStore.get(context)
            ?: return
        PushTokenStore.save(context, token)
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: ""
        NotificationPreferencesRepository().registerDevice(token, "android", version)
    }

    suspend fun unregisterStoredToken(context: Context) {
        val token = PushTokenStore.get(context) ?: return
        runCatching { NotificationPreferencesRepository().unregisterDevice(token) }
        PushTokenStore.clear(context)
    }

    fun onNewToken(context: Context, token: String) {
        PushTokenStore.save(context.applicationContext, token)
        if (FirebaseAuth.getInstance().currentUser != null) {
            GlobalScope.launch(Dispatchers.IO) {
                runCatching { registerCurrentToken(context.applicationContext) }
            }
        }
    }
}
