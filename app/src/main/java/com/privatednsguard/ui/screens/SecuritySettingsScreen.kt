package com.privatednsguard.ui.screens

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.privatednsguard.R
import com.privatednsguard.data.model.DnsPolicyState
import com.privatednsguard.data.security.SecurityManager
import com.privatednsguard.receiver.DnsAdminReceiver
import com.privatednsguard.ui.components.PinAuthDialog
import com.privatednsguard.ui.theme.DarkBorder
import com.privatednsguard.ui.theme.DarkSurface
import com.privatednsguard.ui.theme.DarkSurfaceElevated
import com.privatednsguard.ui.theme.GuardBlue
import com.privatednsguard.ui.theme.GuardGreen
import com.privatednsguard.ui.theme.GuardRed
import com.privatednsguard.ui.theme.TextMuted
import com.privatednsguard.ui.theme.TextPrimary
import com.privatednsguard.ui.theme.TextSecondary

@Composable
fun SecuritySettingsScreen(
    state: DnsPolicyState,
    securityManager: SecurityManager,
    onNavigateToSetup: () -> Unit
) {
    val context = LocalContext.current

    var isPinSet by remember { mutableStateOf(securityManager.isPinSet()) }
    var isBiometricEnabled by remember { mutableStateOf(securityManager.isBiometricEnabled()) }
    var isAlertSoundEnabled by remember { mutableStateOf(securityManager.isAlertSoundEnabled()) }

    var showSetPinDialog by remember { mutableStateOf(false) }
    var showAuthToChangePin by remember { mutableStateOf(false) }
    var showAuthToRemovePin by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Security & Hardening",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Configure app locking, anti-uninstall protection, and alert options.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // App Lock PIN Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = GuardGreen
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Master PIN Lock",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Requires authentication to open the app or disable DNS guard protection.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isPinSet) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showAuthToChangePin = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                            border = BorderStroke(1.dp, DarkBorder),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Change PIN", color = TextPrimary)
                        }

                        Button(
                            onClick = { showAuthToRemovePin = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                            border = BorderStroke(1.dp, GuardRed.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Remove PIN", color = GuardRed)
                        }
                    }

                    // Biometric Option if supported
                    if (securityManager.isBiometricSupported(context)) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newBio = !isBiometricEnabled
                                    isBiometricEnabled = newBio
                                    securityManager.setBiometricEnabled(newBio)
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = if (isBiometricEnabled) GuardGreen else TextMuted
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Biometric Unlock",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Use fingerprint or face recognition",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = {
                                    isBiometricEnabled = it
                                    securityManager.setBiometricEnabled(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = TextPrimary,
                                    checkedTrackColor = GuardGreen
                                )
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { showSetPinDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GuardGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Set Up Master PIN",
                            color = DarkSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Device Admin (Optional Anti-Uninstall)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = GuardBlue
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Anti-Uninstall (Device Admin)",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Optional hardening feature. Prevents uninstallation of PrivateDNS Guard unless deactivated here first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (state.isDeviceAdminActive) "Active (Protected)" else "Inactive (Optional)",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (state.isDeviceAdminActive) GuardGreen else TextMuted,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                            val component = ComponentName(context, DnsAdminReceiver::class.java)

                            if (state.isDeviceAdminActive) {
                                dpm.removeActiveAdmin(component)
                                Toast.makeText(context, "Anti-Uninstall deactivated", Toast.LENGTH_SHORT).show()
                            } else {
                                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                                    putExtra(
                                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                        context.getString(R.string.device_admin_description)
                                    )
                                }
                                context.startActivity(intent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isDeviceAdminActive) DarkSurfaceElevated else GuardBlue
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (state.isDeviceAdminActive) "Deactivate" else "Activate",
                            color = if (state.isDeviceAdminActive) GuardRed else DarkSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Notifications & Alerts Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = GuardGreen
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Tamper Alarm Sound",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Play audio & vibrate when tamper is blocked",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = isAlertSoundEnabled,
                        onCheckedChange = {
                            isAlertSoundEnabled = it
                            securityManager.setAlertSoundEnabled(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextPrimary,
                            checkedTrackColor = GuardGreen
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Battery Optimization Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BatteryAlert,
                        contentDescription = null,
                        tint = GuardGreen
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Background Reliability",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Ensure aggressive OEM battery managers do not kill PrivateDNS Guard or delay boot auto-start.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        try {
                            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                            if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, "Battery optimizations already unrestricted!", Toast.LENGTH_SHORT).show()
                            }
                        } catch (_: Throwable) {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                    border = BorderStroke(1.dp, DarkBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Exempt from Battery Saver", color = TextPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Permissions Wizard Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToSetup() },
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = GuardBlue
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Permission Setup (Shizuku / ADB)",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (state.hasSecureSettingsPermission) "Granted & Active" else "Setup required",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.hasSecureSettingsPermission) GuardGreen else GuardBlue
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
        }
    }

    if (showSetPinDialog) {
        CreatePinDialog(
            onPinCreated = { newPin ->
                securityManager.setPin(newPin)
                isPinSet = true
                showSetPinDialog = false
                Toast.makeText(context, "Master PIN successfully set!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showSetPinDialog = false }
        )
    }

    if (showAuthToChangePin) {
        PinAuthDialog(
            title = "Verify Current PIN",
            subtitle = "Enter your current PIN to set a new one",
            securityManager = securityManager,
            onSuccess = {
                showAuthToChangePin = false
                showSetPinDialog = true
            },
            onDismiss = { showAuthToChangePin = false }
        )
    }

    if (showAuthToRemovePin) {
        PinAuthDialog(
            title = "Verify PIN Removal",
            subtitle = "Enter current PIN to remove app lock",
            securityManager = securityManager,
            onSuccess = {
                securityManager.removePin()
                isPinSet = false
                isBiometricEnabled = false
                showAuthToRemovePin = false
                Toast.makeText(context, "PIN removed", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showAuthToRemovePin = false }
        )
    }
}

@Composable
fun CreatePinDialog(
    onPinCreated: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Create Master PIN",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it },
                    label = { Text("4-digit PIN") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmPin = it },
                    label = { Text("Confirm PIN") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = error ?: "", color = GuardRed, style = MaterialTheme.typography.labelSmall)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (pin.length < 4) {
                                error = "PIN must be at least 4 digits."
                            } else if (pin != confirmPin) {
                                error = "PINs do not match."
                            } else {
                                onPinCreated(pin)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GuardGreen)
                    ) {
                        Text("Save PIN", color = DarkSurface, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
