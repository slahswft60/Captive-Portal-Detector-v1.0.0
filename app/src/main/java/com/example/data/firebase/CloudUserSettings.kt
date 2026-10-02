package com.example.data.firebase

import com.google.firebase.Timestamp

data class CloudUserSettings(
    val userId: String = "",
    val preferredProbeUrl: String = "http://connectivitycheck.android.com/generate_204",
    val autoOpenBrowser: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val updatedAt: Timestamp? = null
)
