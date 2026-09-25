package com.example.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Network
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.example.data.AppDatabase
import com.example.data.PortalEvent
import com.example.notification.NotificationHelper
import com.example.worker.CaptivePortalSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CaptivePortalService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var notificationHelper: NotificationHelper
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var database: AppDatabase

    companion object {
        var currentTargetNetwork: Network? = null
            private set

        @Volatile
        var isRunning: Boolean = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        NetworkMonitorBus.setServiceRunning(true)
        notificationHelper = NotificationHelper(this)
        database = AppDatabase.getInstance(this)

        val notification = notificationHelper
            .buildForegroundNotification("مراقبة اتصال الشبكة نشطة...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NotificationHelper.MONITOR_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NotificationHelper.MONITOR_NOTIFICATION_ID, notification)
        }

        // Log service started event
        serviceScope.launch(Dispatchers.IO) {
            database.portalEventDao().insertEvent(
                PortalEvent(
                    eventType = "SERVICE_START",
                    networkType = "SYSTEM",
                    details = "تم تشغيل خدمة المراقبة في الخلفية"
                )
            )
        }

        networkMonitor = NetworkMonitor(
            context = this,
            scope = serviceScope,
            onPortalDetected = { network, redirectUrl, isRfc8908 ->
                currentTargetNetwork = network
                notificationHelper.showCaptivePortalNotification(redirectUrl)

                serviceScope.launch(Dispatchers.IO) {
                    database.portalEventDao().insertEvent(
                        PortalEvent(
                            eventType = "PORTAL_DETECTED",
                            networkType = "WIFI",
                            redirectUrl = redirectUrl,
                            statusCode = 302,
                            details = if (isRfc8908) "رصد بوابة عبر RFC 8908" else "رصد تحويل لبوابة دخول"
                        )
                    )
                }
            },
            onNetworkStatusChanged = { status ->
                NetworkMonitorBus.setStatusText(status)
                notificationHelper.updateForegroundNotification(status)
            }
        )
        networkMonitor.startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onTimeout(startId: Int) {
        handleTimeout(startId)
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        if (fgsType == ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) {
            handleTimeout(startId)
        }
    }

    private fun handleTimeout(startId: Int) {
        CaptivePortalSyncWorker.schedulePeriodicCheck(applicationContext)
        stopSelf(startId)
    }

    override fun onDestroy() {
        isRunning = false
        NetworkMonitorBus.setServiceRunning(false)
        NetworkMonitorBus.setStatusText("خدمة المراقبة متوقفة")
        networkMonitor.stopMonitoring()
        notificationHelper.dismissCaptivePortalNotification()
        serviceScope.cancel()
        currentTargetNetwork = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
