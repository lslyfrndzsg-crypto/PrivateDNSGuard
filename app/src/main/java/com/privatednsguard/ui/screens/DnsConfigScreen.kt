package com.privatednsguard.ui.screens

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.privatednsguard.data.model.DnsMode
import com.privatednsguard.data.model.DnsPolicyState
import com.privatednsguard.data.model.DnsProviderPreset
import com.privatednsguard.data.model.DnsValidation
import com.privatednsguard.data.model.ValidationResult
import com.privatednsguard.data.repository.DnsGuardRepository
import com.privatednsguard.ui.theme.DarkBorder
import com.privatednsguard.ui.theme.DarkSurface
import com.privatednsguard.ui.theme.DarkSurfaceElevated
import com.privatednsguard.ui.theme.GuardGreen
import com.privatednsguard.ui.theme.GuardRed
import com.privatednsguard.ui.theme.TextPrimary
import com.privatednsguard.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun DnsConfigScreen(
    state: DnsPolicyState,
    repository: DnsGuardRepository
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedMode by remember(state.targetMode) { mutableStateOf(state.targetMode) }
    var hostnameInput by remember(state.targetSpecifier) { mutableStateOf(state.targetSpecifier) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "DNS Policy Configuration",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Specify the exact Private DNS profile that PrivateDNS Guard will enforce and lock.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Sync from System DNS Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val activeMode = repository.readActiveSystemMode()
                    val activeSpec = repository.readActiveSystemSpecifier()
                    selectedMode = activeMode
                    hostnameInput = activeSpec
                    repository.setTargetPolicy(activeMode, activeSpec)
                    Toast
                        .makeText(context, "Synced active Android DNS into guard policy!", Toast.LENGTH_SHORT)
                        .show()
                },
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            border = BorderStroke(1.dp, GuardGreen.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sync",
                    tint = GuardGreen
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Sync From Current Android DNS",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Set to whatever is currently configured in Android Settings",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Mode Selection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Enforced Private DNS Mode",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                DnsMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedMode = mode
                                repository.setTargetPolicy(mode, hostnameInput)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedMode == mode),
                            onClick = {
                                selectedMode = mode
                                repository.setTargetPolicy(mode, hostnameInput)
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = GuardGreen,
                                unselectedColor = TextSecondary
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = mode.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            val desc = when (mode) {
                                DnsMode.HOSTNAME -> "Always forces DNS-over-TLS through your designated provider."
                                DnsMode.OPPORTUNISTIC -> "Opportunistic DoT with network provider fallback."
                                DnsMode.OFF -> "Disables Private DNS."
                            }
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        if (selectedMode == DnsMode.HOSTNAME) {
            Spacer(modifier = Modifier.height(20.dp))

            // Custom Hostname Editor
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Target Hostname (DoT)",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Enter your custom NextDNS endpoint (e.g. xyz123.dns.nextdns.io) or any DoT provider hostname.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = hostnameInput,
                        onValueChange = { newVal ->
                            hostnameInput = newVal.trim()
                            val validation = DnsValidation.validateDoTHostname(newVal)
                            validationError = when (validation) {
                                is ValidationResult.Valid -> {
                                    repository.setTargetPolicy(selectedMode, newVal)
                                    null
                                }
                                is ValidationResult.Invalid -> validation.reason
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = validationError != null,
                        placeholder = { Text("e.g. dns.nextdns.io", color = TextSecondary) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = DarkSurfaceElevated,
                            unfocusedContainerColor = DarkSurfaceElevated,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedIndicatorColor = GuardGreen,
                            unfocusedIndicatorColor = DarkBorder,
                            errorIndicatorColor = GuardRed
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (validationError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = validationError ?: "",
                            color = GuardRed,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val validation = DnsValidation.validateDoTHostname(hostnameInput)
                            if (validation is ValidationResult.Invalid) {
                                validationError = validation.reason
                                return@Button
                            }
                            repository.setTargetPolicy(selectedMode, hostnameInput)
                            coroutineScope.launch {
                                val result = repository.applyTargetDnsToSystem()
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Target applied to Android Settings!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GuardGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = DarkSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save & Apply to Android",
                            color = DarkSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Presets Catalog
            Text(
                text = "Reputable DNS Presets",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            DnsProviderPreset.PRESETS.forEach { preset ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            hostnameInput = preset.hostname
                            repository.setTargetPolicy(DnsMode.HOSTNAME, preset.hostname)
                            validationError = null
                        },
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(
                        1.dp,
                        if (hostnameInput.equals(preset.hostname, ignoreCase = true)) GuardGreen else DarkBorder
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Dns,
                                    contentDescription = null,
                                    tint = GuardGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = preset.hostname,
                                style = MaterialTheme.typography.bodyMedium,
                                color = GuardGreen,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = preset.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }

                        if (hostnameInput.equals(preset.hostname, ignoreCase = true)) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = GuardGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
