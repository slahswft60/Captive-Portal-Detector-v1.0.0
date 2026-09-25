package com.example.service

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

object NetworkMonitorBus {
    @VisibleForTesting
    internal var channel = Channel<Unit>(Channel.CONFLATED)

    val recheckRequests: Flow<Unit>
        get() = channel.receiveAsFlow()

    private val _serviceRunning = MutableStateFlow(false)
    val serviceRunning: StateFlow<Boolean> = _serviceRunning.asStateFlow()

    private val _statusText = MutableStateFlow("الخدمة جاهزة للتشغيل")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _activePortalUrl = MutableStateFlow<String?>(null)
    val activePortalUrl: StateFlow<String?> = _activePortalUrl.asStateFlow()

    private val _networkName = MutableStateFlow("جارٍ التحقق...")
    val networkName: StateFlow<String> = _networkName.asStateFlow()

    fun requestRecheck() {
        channel.trySend(Unit)
    }

    fun setServiceRunning(running: Boolean) {
        _serviceRunning.value = running
    }

    fun setStatusText(status: String) {
        _statusText.value = status
    }

    fun setActivePortalUrl(url: String?) {
        _activePortalUrl.value = url
    }

    fun setNetworkName(name: String) {
        _networkName.value = name
    }

    @VisibleForTesting
    fun resetForTests() {
        channel.close()
        channel = Channel(Channel.CONFLATED)
        _serviceRunning.value = false
        _statusText.value = "الخدمة جاهزة"
        _activePortalUrl.value = null
    }
}
