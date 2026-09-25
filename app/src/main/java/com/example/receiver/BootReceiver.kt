package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.service.CaptivePortalService
import com.example.worker.CaptivePortalSyncWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Schedule background WorkManager task
            CaptivePortalSyncWorker.schedulePeriodicCheck(context.applicationContext)

            // Attempt to start continuous monitor service
            try {
                val serviceIntent = Intent(context, CaptivePortalService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)
            } catch (_: Exception) {
            }
        }
    }
}
