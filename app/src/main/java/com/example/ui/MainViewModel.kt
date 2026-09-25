package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.example.data.AppDatabase
import com.example.data.PortalEvent
import com.example.network.CaptivePortalDetector
import com.example.network.PortalCheckResult
import com.example.service.CaptivePortalService
import com.example.service.NetworkMonitorBus
import com.example.worker.CaptivePortalSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ManualCheckState(
    val isChecking: Boolean = false,
    val result: PortalCheckResult? = null,
    val timestamp: Long = 0L
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val connectivityManager = application.getSystemService(ConnectivityManager::class.java)
    private val detector = CaptivePortalDetector(connectivityManager)

    val serviceRunning: StateFlow<Boolean> = NetworkMonitorBus.serviceRunning
    val statusText: StateFlow<String> = NetworkMonitorBus.statusText
    val activePortalUrl: StateFlow<String?> = NetworkMonitorBus.activePortalUrl
    val networkName: StateFlow<String> = NetworkMonitorBus.networkName

    val recentEvents: StateFlow<List<PortalEvent>> = db.portalEventDao()
        .getAllEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _manualCheckState = MutableStateFlow(ManualCheckState())
    val manualCheckState: StateFlow<ManualCheckState> = _manualCheckState.asStateFlow()

    private val _selectedProbeUrl = MutableStateFlow("http://connectivitycheck.android.com/generate_204")
    val selectedProbeUrl: StateFlow<String> = _selectedProbeUrl.asStateFlow()

    fun setSelectedProbeUrl(url: String) {
        _selectedProbeUrl.value = url
    }

    fun startService() {
        val app = getApplication<Application>()
        val intent = Intent(app, CaptivePortalService::class.java)
        try {
            ContextCompat.startForegroundService(app, intent)
        } catch (_: Exception) {
        }
    }

    fun stopService() {
        val app = getApplication<Application>()
        val intent = Intent(app, CaptivePortalService::class.java)
        app.stopService(intent)
        WorkManager.getInstance(app).cancelUniqueWork(CaptivePortalSyncWorker.WORK_NAME)
    }

    fun runManualProbe(customUrl: String? = null) {
        val probe = customUrl ?: _selectedProbeUrl.value
        _manualCheckState.value = ManualCheckState(isChecking = true)

        viewModelScope.launch(Dispatchers.IO) {
            val activeNet = connectivityManager.activeNetwork
            val checkResult = detector.checkNetwork(activeNet, preferredProbe = probe)

            _manualCheckState.value = ManualCheckState(
                isChecking = false,
                result = checkResult,
                timestamp = System.currentTimeMillis()
            )

            // Persist manual check result in Room
            val (eventType, redirect, code, details) = when (checkResult) {
                is PortalCheckResult.Validated -> {
                    Quadruple("MANUAL_CHECK", null, checkResult.statusCode, "الاتصال سليم (204)")
                }
                is PortalCheckResult.PortalDetected -> {
                    NetworkMonitorBus.setActivePortalUrl(checkResult.redirectUrl)
                    Quadruple("PORTAL_DETECTED", checkResult.redirectUrl, checkResult.statusCode, "تم رصد بوابة دخول عبر الفحص اليدوي")
                }
                is PortalCheckResult.Error -> {
                    Quadruple("ERROR", null, 0, "فشل فحص الاتصال: ${checkResult.exception.message}")
                }
            }

            db.portalEventDao().insertEvent(
                PortalEvent(
                    eventType = eventType,
                    networkType = NetworkMonitorBus.networkName.value,
                    redirectUrl = redirect,
                    statusCode = code,
                    probeUrl = probe,
                    details = details
                )
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            db.portalEventDao().clearAll()
        }
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
