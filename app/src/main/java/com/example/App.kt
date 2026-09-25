package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)

        // Prune old log events asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDatabase.getInstance(this@App).portalEventDao().pruneOldEvents()
            } catch (_: Exception) {
            }
        }
    }
}
