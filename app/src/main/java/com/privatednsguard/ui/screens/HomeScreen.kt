package com.privatednsguard.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privatednsguard.data.model.DnsMode
import com.privatednsguard.data.model.DnsPolicyState
import com.privatednsguard.data.model.GuardStatus
import com.privatednsguard.data.security.SecurityManager
import com.privatednsguard.ui.components.PinAuthDialog
import com.privatednsguard.ui.components.SnoozeDialog
import com.privatednsguard.ui.theme.DarkBorder
import com.privatednsguard.ui.theme.DarkSurface
import com.privatednsguard.ui.theme.DarkSurfaceElevated
import com.privatednsguard.ui.theme.GuardAmber
import com.privatednsguard.ui.theme.GuardAmberGlow
import com.privatednsguard.ui.theme.GuardBlue
import com.privatednsguard.ui.theme.GuardBlueGlow
import com.privatednsguard.ui.theme.GuardGreen
import com.privatednsguard.ui.theme.GuardGreenGlow
import com.privatednsguard.ui.theme.GuardRed
import com.privatednsguard.ui.theme.GuardRedGlow
import com.privatednsguard.ui.theme.TextMuted
import com.privatednsguard.ui.theme.TextPrimary
import com.privatednsguard.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    state: DnsPolicyState,
    securityManager: SecurityManager,
    onToggleGuard: (Boolean) -> Unit,
    onAdoptCurrentSystemDns: () -> Unit,
    onSnoozeSelected: (Int) -> Unit,
    onCancelSnooze: () -> Unit,
    onNavigateToSetup: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onNavigateToConfig: () -> Unit
) {
    var showPinDialogForToggleOff by remember { mutableStateOf(false) }
    var showSnoozeDialog by remember { mutableStateOf(false) }

    val statusColor by animateColorAsState(
        targetValue = when (state.status) {
            GuardStatus.PROTECTED -> GuardGreen
            GuardStatus.SNOOZED -> GuardAmber
            GuardStatus.TAMPER_DETECTED -> GuardRed
            GuardStatus.PERMISSION_REQUIRED -> GuardBlue
            GuardStatus.UNPROTECTED -> TextMuted
        },
        label = "statusColor"
    )

    val glowColor = when (state.status) {
        GuardStatus.PROTECTED -> GuardGreenGlow
        GuardStatus.SNOOZED -> GuardAmberGlow
        GuardStatus.TAMPER_DETECTED -> GuardRedGlow
        GuardStatus.PERMISSION_REQUIRED -> GuardBlueGlow
        GuardStatus.UNPROTECTED -> Color.Transparent
    }

    val statusTitle = when (state.status) {
        GuardStatus.PROTECTED -> "GUARD ACTIVE & LOCKED"
        GuardStatus.SNOOZED -> "PROTECTION PAUSED"
        GuardStatus.TAMPER_DETECTED -> "TAMPER BLOCKED & REVERTING"
        GuardStatus.PERMISSION_REQUIRED -> "PERMISSION REQUIRED"
        GuardStatus.UNPROTECTED -> "GUARD DISARMED"
    }

    val statusSubtitle = when (state.status) {
        GuardStatus.PROTECTED -> "Android Private DNS is securely monitored and locked."
        GuardStatus.SNOOZED -> "${state.snoozeRemainingSeconds / 60 + 1} minutes remaining before auto-relock."
        GuardStatus.TAMPER_DETECTED -> "Unauthorized change caught. Restoring target policy."
        GuardStatus.PERMISSION_REQUIRED -> "WRITE_SECURE_SETTINGS is needed to monitor and lock DNS."
        GuardStatus.UNPROTECTED -> "DNS can be freely changed in Android settings."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Permission Callout Banner if needed
        AnimatedVisibility(visible = !state.hasSecureSettingsPermission) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable { onNavigateToSetup() },
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = BorderStroke(1.dp, GuardBlue),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Permission Needed",
                        tint = GuardBlue,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Permission Required",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap to grant WRITE_SECURE_SETTINGS with 1-tap Shizuku or ADB command.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Go",
                        tint = GuardBlue
                    )
                }
            }
        }

        // Hero Security Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.5.dp, statusColor.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(glowColor)
                        .border(2.dp, statusColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.isGuardEnabled) Icons.Default.Shield else Icons.Default.Lock,
                        contentDescription = "Shield",
                        tint = statusColor,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = statusTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = statusSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Master Toggle Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceElevated)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Guard Protection",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (state.isGuardEnabled) "Auto-revert enabled" else "Protection turned off",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.isGuardEnabled) GuardGreen else TextMuted
                        )
                    }

                    Switch(
                        checked = state.isGuardEnabled,
                        onCheckedChange = { willEnable ->
                            if (willEnable) {
                                onToggleGuard(true)
                            } else {
                                if (securityManager.isPinSet()) {
                                    showPinDialogForToggleOff = true
                                } else {
                                    onToggleGuard(false)
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextPrimary,
                            checkedTrackColor = GuardGreen,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkSurface
                        )
                    )
                }

                // Snooze / Pause Button
                if (state.isGuardEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (state.snoozeRemainingSeconds > 0) {
                        OutlinedButton(
                            onClick = onCancelSnooze,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GuardAmber),
                            border = BorderStroke(1.dp, GuardAmber),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Resume Protection Now")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { showSnoozeDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = BorderStroke(1.dp, DarkBorder),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pause for Captive Portal / Hotel Wi-Fi")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // "Lock Current System DNS" One-Tap Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BookmarkAdded,
                        contentDescription = "Adopt System DNS",
                        tint = GuardGreen
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Current Android DNS Setting",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val activeDesc = if (state.activeMode == DnsMode.HOSTNAME) {
                    state.activeSpecifier.ifBlank { "(No Hostname Set)" }
                } else {
                    state.activeMode.displayName
                }

                Text(
                    text = "System Value: $activeDesc",
                    style = MaterialTheme.typography.bodyLarge,
                    color = GuardGreen,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                val targetDesc = if (state.targetMode == DnsMode.HOSTNAME) {
                    state.targetSpecifier
                } else {
                    state.targetMode.displayName
                }

                Text(
                    text = "Protected Policy Target: $targetDesc",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onAdoptCurrentSystemDns,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                    border = BorderStroke(1.dp, DarkBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Lock Current Setting as Guard Target",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Tamper Statistics Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToLogs() },
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Tamper Attempts Blocked",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${state.blockedTamperCount} incidents logged and reverted",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (state.blockedTamperCount > 0) GuardAmber else DarkSurfaceElevated)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${state.blockedTamperCount}",
                            color = if (state.blockedTamperCount > 0) DarkSurface else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "View Logs",
                        tint = TextSecondary
                    )
                }
            }
        }
    }

    if (showPinDialogForToggleOff) {
        PinAuthDialog(
            title = "Confirm Disarm",
            subtitle = "Enter PIN to turn off Private DNS Guard protection",
            securityManager = securityManager,
            onSuccess = {
                showPinDialogForToggleOff = false
                onToggleGuard(false)
            },
            onDismiss = { showPinDialogForToggleOff = false }
        )
    }

    if (showSnoozeDialog) {
        SnoozeDialog(
            onSnoozeSelected = { minutes ->
                showSnoozeDialog = false
                onSnoozeSelected(minutes)
            },
            onDismiss = { showSnoozeDialog = false }
        )
    }
}
