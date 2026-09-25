package com.example.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.CaptivePortalLoginActivity
import com.example.MainActivity
import com.example.R

class NotificationHelper(
    private val context: Context,
    private val clock: () -> Long = System::currentTimeMillis,
    private val dedupeWindowMs: Long = 60_000L
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var lastNotifiedUrl: String? = null
    private var lastNotifiedTimestamp = 0L

    companion object {
        const val MONITOR_CHANNEL_ID = "channel_network_monitor"
        const val PORTAL_CHANNEL_ID = "channel_captive_portal"
        const val MONITOR_NOTIFICATION_ID = 1001
        const val PORTAL_NOTIFICATION_ID = 1002

        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(NotificationManager::class.java) ?: return

                val monitorChannel = NotificationChannel(
                    MONITOR_CHANNEL_ID,
                    context.getString(R.string.notif_channel_monitor),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "يعرض حالة خدمة فحص اتصال الشبكة في الخلفية"
                    setShowBadge(false)
                }

                val portalChannel = NotificationChannel(
                    PORTAL_CHANNEL_ID,
                    context.getString(R.string.notif_channel_portal),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "تنبيه فوري عند رصد بوابة تتطلب تسجيل الدخول للوصول إلى الإنترنت"
                    enableVibration(true)
                    setShowBadge(true)
                }

                manager.createNotificationChannel(monitorChannel)
                manager.createNotificationChannel(portalChannel)
            }
        }
    }

    fun buildForegroundNotification(statusText: String): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, MONITOR_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notif_monitor_title))
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    fun updateForegroundNotification(statusText: String) {
        notificationManager.notify(MONITOR_NOTIFICATION_ID, buildForegroundNotification(statusText))
    }

    fun showCaptivePortalNotification(redirectUrl: String?): Boolean {
        val now = clock()
        if (redirectUrl != null && redirectUrl == lastNotifiedUrl && (now - lastNotifiedTimestamp < dedupeWindowMs)) {
            return false
        }
        lastNotifiedUrl = redirectUrl
        lastNotifiedTimestamp = now

        val loginIntent = Intent(context, CaptivePortalLoginActivity::class.java).apply {
            putExtra(CaptivePortalLoginActivity.EXTRA_PORTAL_URL, redirectUrl)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2,
            loginIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, PORTAL_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notif_portal_title))
            .setContentText(context.getString(R.string.notif_portal_text))
            .setSubText(redirectUrl?.take(70))
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_send,
                context.getString(R.string.btn_open_portal),
                pendingIntent
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        notificationManager.notify(PORTAL_NOTIFICATION_ID, notification)
        return true
    }

    fun dismissCaptivePortalNotification() {
        notificationManager.cancel(PORTAL_NOTIFICATION_ID)
    }
}
