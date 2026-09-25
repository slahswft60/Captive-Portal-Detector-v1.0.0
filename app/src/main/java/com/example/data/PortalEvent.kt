package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "portal_events")
data class PortalEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String, // "PORTAL_DETECTED", "VALIDATED", "NETWORK_LOST", "MANUAL_CHECK", "SERVICE_START"
    val networkType: String, // "WIFI", "CELLULAR", "VPN", "ETHERNET", "NONE"
    val redirectUrl: String? = null,
    val statusCode: Int = 0,
    val probeUrl: String = "",
    val details: String = ""
)
