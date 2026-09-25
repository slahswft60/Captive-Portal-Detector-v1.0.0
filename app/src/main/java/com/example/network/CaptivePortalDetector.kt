package com.example.network

import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

sealed class PortalCheckResult {
    data class Validated(
        val statusCode: Int = 204,
        val probeUrl: String = ""
    ) : PortalCheckResult()

    data class PortalDetected(
        val redirectUrl: String?,
        val statusCode: Int,
        val isRfc8908: Boolean = false,
        val probeUrl: String = ""
    ) : PortalCheckResult()

    data class Error(
        val exception: Throwable,
        val probeUrl: String = ""
    ) : PortalCheckResult()
}

fun interface ConnectionOpener {
    fun open(url: URL, network: Network?): HttpURLConnection
}

class CaptivePortalDetector(
    private val connectivityManager: ConnectivityManager?,
    private val opener: ConnectionOpener = ConnectionOpener { url, network ->
        val conn = if (network != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            network.openConnection(url) as HttpURLConnection
        } else {
            url.openConnection() as HttpURLConnection
        }
        conn.apply {
            connectTimeout = 4000
            readTimeout = 4000
            instanceFollowRedirects = false
            useCaches = false
            setRequestProperty("User-Agent", "Android/CaptivePortalDetector")
        }
    }
) {
    var probeEndpoints: List<String> = listOf(
        "http://connectivitycheck.android.com/generate_204",
        "http://connectivitycheck.gstatic.com/generate_204",
        "http://clients3.google.com/generate_204",
        "http://cp.cloudflare.com/generate_204"
    )

    suspend fun checkNetwork(network: Network?, preferredProbe: String? = null): PortalCheckResult =
        withContext(Dispatchers.IO) {
            // 1. RFC 8908 check on Android 12+ (API 31+) via reflection
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && network != null && connectivityManager != null) {
                try {
                    val linkProps = connectivityManager.getLinkProperties(network)
                    if (linkProps != null) {
                        val getCaptiveMethod = linkProps.javaClass.methods.firstOrNull { it.name == "getCaptivePortalData" }
                        val portalData = getCaptiveMethod?.invoke(linkProps)
                        if (portalData != null) {
                            val isCaptiveMethod = portalData.javaClass.methods.firstOrNull { it.name == "isCaptive" }
                            val isCaptive = (isCaptiveMethod?.invoke(portalData) as? Boolean) == true
                            if (isCaptive) {
                                val getUrlMethod = portalData.javaClass.methods.firstOrNull { it.name == "getUserPortalUrl" }
                                val portalUrl = getUrlMethod?.invoke(portalData)?.toString()
                                return@withContext PortalCheckResult.PortalDetected(
                                    redirectUrl = portalUrl,
                                    statusCode = 302,
                                    isRfc8908 = true,
                                    probeUrl = "RFC 8908 LinkProperties"
                                )
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Fall back to HTTP probe
                }
            }

            // 2. HTTP Probes
            val endpointsToTry = if (!preferredProbe.isNullOrBlank()) {
                listOf(preferredProbe) + probeEndpoints.filter { it != preferredProbe }
            } else {
                probeEndpoints
            }

            var lastException: Throwable? = null
            var lastTriedUrl = endpointsToTry.firstOrNull() ?: ""

            for (endpoint in endpointsToTry) {
                lastTriedUrl = endpoint
                var connection: HttpURLConnection? = null
                try {
                    val url = URL(endpoint)
                    connection = opener.open(url, network)
                    val responseCode = connection.responseCode

                    when (responseCode) {
                        204 -> {
                            connection.disconnect()
                            return@withContext PortalCheckResult.Validated(
                                statusCode = 204,
                                probeUrl = endpoint
                            )
                        }
                        301, 302, 303, 307, 308 -> {
                            val location = connection.getHeaderField("Location")
                            connection.disconnect()
                            return@withContext PortalCheckResult.PortalDetected(
                                redirectUrl = location ?: endpoint,
                                statusCode = responseCode,
                                isRfc8908 = false,
                                probeUrl = endpoint
                            )
                        }
                        200 -> {
                            // Probe expected 204 No Content, but returned 200 OK -> captive portal served login HTML
                            connection.disconnect()
                            return@withContext PortalCheckResult.PortalDetected(
                                redirectUrl = endpoint,
                                statusCode = 200,
                                isRfc8908 = false,
                                probeUrl = endpoint
                            )
                        }
                        else -> {
                            connection.disconnect()
                        }
                    }
                } catch (e: Exception) {
                    lastException = e
                    try {
                        connection?.disconnect()
                    } catch (_: Exception) {
                    }
                }
            }

            PortalCheckResult.Error(
                exception = lastException ?: Exception("تعذر الاتصال بخوادم الفحص"),
                probeUrl = lastTriedUrl
            )
        }
}
