package com.privatednsguard.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privatednsguard.data.model.DnsPolicyState
import com.privatednsguard.data.repository.DnsGuardRepository
import com.privatednsguard.shizuku.ShizukuManager
import com.privatednsguard.ui.theme.DarkBorder
import com.privatednsguard.ui.theme.DarkSurface
import com.privatednsguard.ui.theme.DarkSurfaceElevated
import com.privatednsguard.ui.theme.GuardAmber
import com.privatednsguard.ui.theme.GuardBlue
import com.privatednsguard.ui.theme.GuardGreen
import com.privatednsguard.ui.theme.GuardRed
import com.privatednsguard.ui.theme.TextPrimary
import com.privatednsguard.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun SetupWizardScreen(
    state: DnsPolicyState,
    repository: DnsGuardRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isGrantingViaShizuku by remember { mutableStateOf(false) }

    val isBinderReceived by ShizukuManager.isBinderReceived.collectAsState()
    val hasShizukuPermission by ShizukuManager.hasPermission.collectAsState()

    val isShizukuRunning = isBinderReceived || ShizukuManager.isShizukuRunning()
    val isShizukuAuthorized = hasShizukuPermission || ShizukuManager.hasShizukuPermission()

    val adbCommand = "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Permission Setup",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Permission Status Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (state.hasSecureSettingsPermission) GuardGreen.copy(alpha = 0.15f) else GuardBlue.copy(alpha = 0.15f)
            ),
            border = BorderStroke(
                1.dp,
                if (state.hasSecureSettingsPermission) GuardGreen else GuardBlue
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (state.hasSecureSettingsPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (state.hasSecureSettingsPermission) GuardGreen else GuardBlue,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = if (state.hasSecureSettingsPermission) "Permission Active & Ready" else "WRITE_SECURE_SETTINGS Missing",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (state.hasSecureSettingsPermission) {
                            "PrivateDNS Guard has elevated rights to lock and restore DNS."
                        } else {
                            "Required to monitor and revert Android Private DNS settings."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Method 1: Shizuku (1-Tap Setup, No PC)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, if (state.hasSecureSettingsPermission) GuardGreen.copy(alpha = 0.5f) else DarkBorder),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = GuardGreen
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Method 1: Shizuku (No PC)",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = {
                            ShizukuManager.checkAndUpdate()
                            repository.refreshState()
                            Toast.makeText(context, "Checked Shizuku status", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "If you have started Shizuku via Wireless Debugging, you can grant the required WRITE_SECURE_SETTINGS permission directly on your phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Shizuku status indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceElevated)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                when {
                                    state.hasSecureSettingsPermission -> GuardGreen
                                    isShizukuRunning && isShizukuAuthorized -> GuardGreen
                                    isShizukuRunning -> GuardAmber
                                    else -> GuardAmber
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = when {
                                state.hasSecureSettingsPermission -> "Secure settings permission already granted!"
                                !isShizukuRunning -> "Shizuku service is not running"
                                !isShizukuAuthorized -> "Shizuku connected • Authorization needed"
                                else -> "Shizuku connected & authorized"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                state.hasSecureSettingsPermission -> GuardGreen
                                isShizukuRunning && isShizukuAuthorized -> GuardGreen
                                else -> GuardAmber
                            },
                            fontWeight = FontWeight.Medium
                        )
                        if (!state.hasSecureSettingsPermission && !isShizukuRunning) {
                            Text(
                                text = "Start Shizuku via Wireless Debugging first, then tap Refresh",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (state.hasSecureSettingsPermission) {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GuardGreen.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = GuardGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Permission Granted & Active",
                            color = GuardGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (!isShizukuRunning) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val launchIntent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                                if (launchIntent != null) {
                                    context.startActivity(launchIntent)
                                } else {
                                    try {
                                        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/"))
                                        context.startActivity(marketIntent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Shizuku is not installed.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, DarkBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Open Shizuku", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                ShizukuManager.checkAndUpdate()
                                repository.refreshState()
                                Toast.makeText(
                                    context,
                                    if (ShizukuManager.isShizukuRunning()) "Connected to Shizuku!" else "Still not detected. Ensure Shizuku is running.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GuardBlue)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = DarkSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Re-check", color = DarkSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                } else if (!isShizukuAuthorized) {
                    Button(
                        onClick = {
                            ShizukuManager.requestShizukuPermission()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GuardAmber)
                    ) {
                        Text(
                            text = "Authorize PrivateDNS Guard in Shizuku",
                            color = DarkSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            isGrantingViaShizuku = true
                            coroutineScope.launch {
                                val result = ShizukuManager.grantWriteSecureSettings(context.packageName)
                                isGrantingViaShizuku = false
                                if (result.isSuccess) {
                                    repository.adoptCurrentSystemDns()
                                    repository.setGuardEnabled(true)
                                    com.privatednsguard.service.PrivateDnsWatchdogService.start(context)
                                    repository.refreshState()
                                    Toast.makeText(context, "Permission granted! Guard activated & locking DNS.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isGrantingViaShizuku,
                        colors = ButtonDefaults.buttonColors(containerColor = GuardGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isGrantingViaShizuku) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = DarkSurface,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = "Grant System Permission (1-Tap)",
                            color = DarkSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Method 2: ADB Command (With PC)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Laptop,
                        contentDescription = null,
                        tint = GuardBlue
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Method 2: ADB Command (Via Computer)",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1. Enable Developer Options & USB Debugging on your phone.\n" +
                            "2. Connect your phone to your computer.\n" +
                            "3. Run the following command in terminal or PowerShell:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Command Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceElevated)
                        .padding(14.dp)
                ) {
                    Text(
                        text = adbCommand,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("ADB Command", adbCommand)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Command copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = BorderStroke(1.dp, DarkBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy ADB Command")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        repository.refreshState()
                        if (repository.hasSecureSettingsPermission()) {
                            Toast.makeText(context, "Permission detected! Guard is active.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Permission not yet detected. Run the command and check again.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GuardBlue),
                    border = BorderStroke(1.dp, GuardBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Check Permission Status")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Step 3: Background Reliability (Battery Optimization)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(
                1.dp,
                if (state.isBatteryOptimizationIgnored) GuardGreen.copy(alpha = 0.5f) else GuardAmber.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BatteryAlert,
                        contentDescription = null,
                        tint = if (state.isBatteryOptimizationIgnored) GuardGreen else GuardAmber
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Essential: 24/7 Background Reliability",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Android battery savers and OEM task managers (Samsung, Xiaomi, OnePlus) aggressively stop background services when the phone is idle or locked. Exempting PrivateDNS Guard ensures protection never sleeps.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (state.isBatteryOptimizationIgnored) {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GuardGreen.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = GuardGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unrestricted Battery Active",
                            color = GuardGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (_: Throwable) {
                                try {
                                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    context.startActivity(intent)
                                } catch (_: Throwable) {
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GuardAmber),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Set Battery to Unrestricted (1-Tap)",
                            color = DarkSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
