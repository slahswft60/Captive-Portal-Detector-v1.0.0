package com.example.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import com.example.network.CaptivePortalDetector
import com.example.network.PortalCheckResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class NetworkMonitor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onPortalDetected: (Network, String?, Boolean) -> Unit,
    private val onNetworkStatusChanged: (String) -> Unit
) {
    private val connectivityManager: ConnectivityManager =
        context.getSystemService(ConnectivityManager::class.java)
    private val detector = CaptivePortalDetector(connectivityManager)

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var busJob: Job? = null
    private var activeNetworkRef: Network? = null
    private var lastCheckTimestamp = 0L
    private var lastResult: PortalCheckResult? = null
    private val minIntervalMs = 3000L

    fun startMonitoring() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                activeNetworkRef = network
                updateNetworkName(network)
                evaluateNetwork(network, force = false)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                activeNetworkRef = network
                updateNetworkName(network)

                val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                val isPortal = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)

                if (isPortal || (hasInternet && !isValidated)) {
                    evaluateNetwork(network, force = false)
                } else if (isValidated) {
                    lastResult = PortalCheckResult.Validated(204, "")
                    NetworkMonitorBus.setActivePortalUrl(null)
                    onNetworkStatusChanged("الاتصال بالإنترنت سليم (204 OK)")
                }
            }

            override fun onLost(network: Network) {
                if (activeNetworkRef == network) {
                    activeNetworkRef = null
                    lastResult = null
                    NetworkMonitorBus.setActivePortalUrl(null)
                    NetworkMonitorBus.setNetworkName("تم قطع الاتصال")
                }
                onNetworkStatusChanged("تم فقدان الاتصال بالشبكة")
            }
        }

        connectivityManager.registerNetworkCallback(request, networkCallback!!)

        // Check active network immediately upon starting
        val current = connectivityManager.activeNetwork
        if (current != null) {
            activeNetworkRef = current
            updateNetworkName(current)
            evaluateNetwork(current, force = true)
        } else {
            NetworkMonitorBus.setNetworkName("لا توجد شبكة متصلة")
            onNetworkStatusChanged("في انتظار الاتصال بشبكة...")
        }

        // Listen for manual re-check requests from UI / LoginActivity
        busJob = scope.launch {
            NetworkMonitorBus.recheckRequests.collect {
                val target = activeNetworkRef ?: connectivityManager.activeNetwork
                target?.let { evaluateNetwork(it, force = true) }
            }
        }
    }

    private fun updateNetworkName(network: Network) {
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return
        val name = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                "شبكة واي فاي (Wi-Fi)"
            }
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                "شبكة بيانات الهاتف (Cellular)"
            }
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                "شبكة سلكية (Ethernet)"
            }
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> {
                "شبكة افتراضية (VPN)"
            }
            else -> "شبكة غير معروفة"
        }
        NetworkMonitorBus.setNetworkName(name)
    }

    fun evaluateNetwork(network: Network, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && (now - lastCheckTimestamp < minIntervalMs)) {
            return
        }
        lastCheckTimestamp = now

        scope.launch {
            onNetworkStatusChanged("جارٍ فحص بوابة تسجيل الدخول...")
            val result = detector.checkNetwork(network)
            if (result == lastResult && !force) return@launch
            lastResult = result

            when (result) {
                is PortalCheckResult.Validated -> {
                    NetworkMonitorBus.setActivePortalUrl(null)
                    onNetworkStatusChanged("الاتصال بالإنترنت سليم وموثّق")
                }
                is PortalCheckResult.PortalDetected -> {
                    val statusText = if (result.isRfc8908) {
                        "تم رصد بوابة دخول (RFC 8908)"
                    } else {
                        "تم رصد بوابة دخول (${result.statusCode})"
                    }
                    NetworkMonitorBus.setActivePortalUrl(result.redirectUrl)
                    onNetworkStatusChanged(statusText)
                    onPortalDetected(network, result.redirectUrl, result.isRfc8908)
                }
                is PortalCheckResult.Error -> {
                    onNetworkStatusChanged("تعذر استكمال فحص الاتصال")
                }
            }
        }
    }

    fun stopMonitoring() {
        busJob?.cancel()
        busJob = null
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (_: Exception) {
            }
            networkCallback = null
        }
    }
}
