package com.example.data.firebase

import com.google.firebase.Timestamp

data class CloudPortalEvent(
    val id: String = "",
    val userId: String = "",
    val eventType: String = "",
    val networkType: String = "",
    val redirectUrl: String? = null,
    val statusCode: Int = 0,
    val probeUrl: String = "",
    val details: String = "",
    val timestamp: Timestamp? = null
)
