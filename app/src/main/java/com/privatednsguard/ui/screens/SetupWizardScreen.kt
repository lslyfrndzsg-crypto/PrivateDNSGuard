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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Laptop
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
            border = BorderStroke(1.dp, DarkBorder),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = GuardGreen
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Method 1: Shizuku (Recommended, No PC)",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "If you have the open-source Shizuku app installed on your phone, you can grant the permission instantly with a single tap.",
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
                    val shizukuRunning = ShizukuManager.isShizukuRunning()
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (shizukuRunning) GuardGreen else GuardAmber)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (shizukuRunning) "Shizuku service is running" else "Shizuku service is not running",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (shizukuRunning) GuardGreen else GuardAmber,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (!ShizukuManager.isShizukuRunning()) {
                            Toast.makeText(context, "Please start the Shizuku app first.", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (!ShizukuManager.hasShizukuPermission()) {
                            ShizukuManager.requestShizukuPermission()
                            return@Button
                        }

                        isGrantingViaShizuku = true
                        coroutineScope.launch {
                            val result = ShizukuManager.grantWriteSecureSettings(context.packageName)
                            isGrantingViaShizuku = false
                            if (result.isSuccess) {
                                repository.refreshState()
                                Toast.makeText(context, "Permission successfully granted!", Toast.LENGTH_SHORT).show()
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
                        text = "Grant Permission via Shizuku",
                        color = DarkSurface,
                        fontWeight = FontWeight.Bold
                    )
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
    }
}
