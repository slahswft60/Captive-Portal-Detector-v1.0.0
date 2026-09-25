package com.example.ui

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.CaptivePortalLoginActivity
import com.example.data.PortalEvent
import com.example.network.PortalCheckResult
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.EmeraldSuccess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onRequestPermissionAndStart: () -> Unit
) {
    val context = LocalContext.current
    val serviceRunning by viewModel.serviceRunning.collectAsState()
    val statusText by viewModel.statusText.collectAsState()
    val activePortalUrl by viewModel.activePortalUrl.collectAsState()
    val networkName by viewModel.networkName.collectAsState()
    val recentEvents by viewModel.recentEvents.collectAsState()
    val manualCheckState by viewModel.manualCheckState.collectAsState()
    val selectedProbeUrl by viewModel.selectedProbeUrl.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "كاشف بوابات الدخول",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Captive Portal Detector",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showInfoDialog = true },
                        modifier = Modifier.testTag("btn_info")
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "معلومات")
                    }
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("btn_settings")
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "الإعدادات")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Service & Network Status Card
            item {
                ServiceStatusCard(
                    isRunning = serviceRunning,
                    networkName = networkName,
                    statusText = statusText,
                    onToggleService = {
                        if (serviceRunning) {
                            viewModel.stopService()
                        } else {
                            onRequestPermissionAndStart()
                        }
                    }
                )
            }

            // 2. Active Captive Portal Alert Card (if detected)
            if (activePortalUrl != null) {
                item {
                    PortalDetectedCard(
                        portalUrl = activePortalUrl!!,
                        onOpenBrowser = {
                            val intent = Intent(context, CaptivePortalLoginActivity::class.java).apply {
                                putExtra(CaptivePortalLoginActivity.EXTRA_PORTAL_URL, activePortalUrl)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // 3. Quick Action Buttons
            item {
                ActionCard(
                    isChecking = manualCheckState.isChecking,
                    onTestNow = { viewModel.runManualProbe() },
                    activePortalUrl = activePortalUrl,
                    onOpenPortal = {
                        val intent = Intent(context, CaptivePortalLoginActivity::class.java).apply {
                            putExtra(CaptivePortalLoginActivity.EXTRA_PORTAL_URL, activePortalUrl)
                        }
                        context.startActivity(intent)
                    }
                )
            }

            // 4. Manual Probe Result Banner
            if (manualCheckState.result != null && !manualCheckState.isChecking) {
                item {
                    ManualCheckResultCard(result = manualCheckState.result!!)
                }
            }

            // 5. Detection Logs & History Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "سجل الأحداث والشبكات",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${recentEvents.size} حدث مسجل محلياً",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (recentEvents.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearHistory() },
                            modifier = Modifier.testTag("btn_clear_history")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "مسح السجل",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // 6. Recent Event Items
            if (recentEvents.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد أحداث مسجلة بعد. شغّل الخدمة لمراقبة الشبكة.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(recentEvents, key = { it.id }) { event ->
                    EventItemCard(event = event)
                }
            }
        }
    }

    // Settings Dialog
    if (showSettingsDialog) {
        ProbeSettingsDialog(
            currentProbe = selectedProbeUrl,
            onSelectProbe = {
                viewModel.setSelectedProbeUrl(it)
                showSettingsDialog = false
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    // Info Dialog
    if (showInfoDialog) {
        InformationDialog(onDismiss = { showInfoDialog = false })
    }
}

@Composable
fun ServiceStatusCard(
    isRunning: Boolean,
    networkName: String,
    statusText: String,
    onToggleService: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val cardColor by animateColorAsState(
        targetValue = if (isRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        label = "cardColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("service_status_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .then(if (isRunning) Modifier.scale(pulseScale) else Modifier)
                            .clip(CircleShape)
                            .background(if (isRunning) EmeraldSuccess else MaterialTheme.colorScheme.outline)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRunning) "الخدمة قيد التشغيل (Foreground)" else "الخدمة متوقفة",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isRunning) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onToggleService,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_toggle_service")
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isRunning) "إيقاف" else "تشغيل")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = networkName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun PortalDetectedCard(
    portalUrl: String,
    onOpenBrowser: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("portal_detected_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AmberAlert.copy(alpha = 0.15f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = AmberAlert,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تم رصد بوابة تسجيل دخول (Captive Portal)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AmberAlert
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "الشبكة الحالية تمنع الوصول للإنترنت حتى يتم إتمام المصادقة وتسجيل الدخول.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = portalUrl,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onOpenBrowser,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_open_portal_now"),
                colors = ButtonDefaults.buttonColors(containerColor = AmberAlert)
            ) {
                Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("فتح صفحة تسجيل الدخول في المتصفح الداخلي", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ActionCard(
    isChecking: Boolean,
    onTestNow: () -> Unit,
    activePortalUrl: String?,
    onOpenPortal: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onTestNow,
            enabled = !isChecking,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("btn_check_now"),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isChecking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("جارٍ الفحص...")
            } else {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("فحص الاتصال الآن")
            }
        }

        if (activePortalUrl != null) {
            FilledTonalButton(
                onClick = onOpenPortal,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("btn_portal_browser"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("المتصفح")
            }
        }
    }
}

@Composable
fun ManualCheckResultCard(result: PortalCheckResult) {
    val (title, body, color, icon) = when (result) {
        is PortalCheckResult.Validated -> Quad(
            "الاتصال بالإنترنت موثّق وسليم (204 No Content)",
            "تم التحقق بنجاح من الخادم: ${result.probeUrl}",
            EmeraldSuccess,
            Icons.Default.CheckCircle
        )
        is PortalCheckResult.PortalDetected -> Quad(
            "تم اكتشاف بوابة دخول (كود ${result.statusCode})",
            "تم التحويل إلى: ${result.redirectUrl ?: result.probeUrl}",
            AmberAlert,
            Icons.Default.Warning
        )
        is PortalCheckResult.Error -> Quad(
            "فشل الاتصال بخادم الفحص",
            result.exception.message ?: "تحقق من اتصال الشبكة",
            MaterialTheme.colorScheme.error,
            Icons.Default.Warning
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun EventItemCard(event: PortalEvent) {
    val sdf = remember { SimpleDateFormat("HH:mm:ss  yyyy-MM-dd", Locale.getDefault()) }
    val timeFormatted = sdf.format(Date(event.timestamp))

    val (badgeText, badgeColor) = when (event.eventType) {
        "PORTAL_DETECTED" -> Pair("بوابة دخول", AmberAlert)
        "VALIDATED" -> Pair("موثّق", EmeraldSuccess)
        "MANUAL_CHECK" -> Pair("فحص يدوي", MaterialTheme.colorScheme.primary)
        "SERVICE_START" -> Pair("تشغيل الخدمة", MaterialTheme.colorScheme.secondary)
        else -> Pair(event.eventType, MaterialTheme.colorScheme.outline)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = event.details.ifBlank { "حدث شبكة: ${event.eventType}" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            if (!event.redirectUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.redirectUrl,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ProbeSettingsDialog(
    currentProbe: String,
    onSelectProbe: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val probeOptions = listOf(
        "Google Android (generate_204)" to "http://connectivitycheck.android.com/generate_204",
        "Google GStatic (generate_204)" to "http://connectivitycheck.gstatic.com/generate_204",
        "Cloudflare Portal Check" to "http://cp.cloudflare.com/generate_204",
        "Google Clients3" to "http://clients3.google.com/generate_204"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اختيار خادم فحص البوابة") },
        text = {
            Column {
                Text(
                    text = "يتم إرسال طلب HTTP إلى أحد هذه الخوادم. إذا ردّ الخادم بكود 204 فالإنترنت متاح. إذا أعيد توجيهك (302) يتم رصد بوابة الدخول.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                probeOptions.forEach { (label, url) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectProbe(url) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (currentProbe == url),
                            onClick = { onSelectProbe(url) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(text = url, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
fun InformationDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("عن آلية كشف بوابات الدخول") },
        text = {
            Column {
                Text(
                    text = "• خدمة الخلفية المستمرة:\nتعمل بنوع Foreground Service (dataSync) لمراقبة تغيّر شبكات الواي فاي لحظة بلحظة.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• كشف بوابات المصادقة (Captive Portals):\nيعتمد التطبيق على معيار RFC 8908 وفحص HTTP 204 التلقائي للكشف عن صفحات الفنادق والمقاهي والمطارات.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• المتصفح الداخلي المقيّد بالشبكة:\nيستخدم bindProcessToNetwork لضمان توجيه حركة المرور حصرياً عبر الشبكة غير المصادق عليها لإتمام الدخول بنجاح.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("حسناً")
            }
        }
    )
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
