package com.example.worker

import android.content.Context
import android.net.ConnectivityManager
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.data.PortalEvent
import com.example.network.CaptivePortalDetector
import com.example.network.PortalCheckResult
import com.example.notification.NotificationHelper
import java.util.concurrent.TimeUnit

class CaptivePortalSyncWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val connectivityManager = appContext
            .getSystemService(ConnectivityManager::class.java)
        val activeNetwork = connectivityManager.activeNetwork ?: return Result.success()

        val detector = CaptivePortalDetector(connectivityManager)
        val result = detector.checkNetwork(activeNetwork)

        if (result is PortalCheckResult.PortalDetected) {
            val notificationHelper = NotificationHelper(appContext)
            notificationHelper.showCaptivePortalNotification(result.redirectUrl)

            try {
                val db = AppDatabase.getInstance(appContext)
                db.portalEventDao().insertEvent(
                    PortalEvent(
                        eventType = "PORTAL_DETECTED",
                        networkType = "BACKGROUND_WORKER",
                        redirectUrl = result.redirectUrl,
                        statusCode = result.statusCode,
                        details = "تم الكشف عبر فحص WorkManager المجدول"
                    )
                )
            } catch (_: Exception) {
            }
        }
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "captive_portal_periodic_check"

        fun schedulePeriodicCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<CaptivePortalSyncWorker>(
                15, TimeUnit.MINUTES
            ).setConstraints(constraints).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun scheduleImmediateCheck(context: Context) {
            val request = OneTimeWorkRequestBuilder<CaptivePortalSyncWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
