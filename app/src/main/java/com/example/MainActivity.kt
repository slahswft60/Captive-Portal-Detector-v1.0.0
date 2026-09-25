package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.service.CaptivePortalService
import com.example.service.NetworkMonitorBus
import com.example.ui.HomeScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startMonitorService()
        } else {
            Toast.makeText(
                this,
                "إذن الإشعارات مطلوب لإرسال تنبيه عند اكتشاف بوابة تسجيل دخول",
                Toast.LENGTH_LONG
            ).show()
            // Still start service (notifications may be silent on Android 13+)
            startMonitorService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                HomeScreen(
                    viewModel = viewModel,
                    onRequestPermissionAndStart = {
                        checkNotificationPermissionAndStart()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        NetworkMonitorBus.requestRecheck()
    }

    private fun checkNotificationPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        startMonitorService()
    }

    private fun startMonitorService() {
        viewModel.startService()
    }
}
