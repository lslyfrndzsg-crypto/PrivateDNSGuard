package com.privatednsguard.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.privatednsguard.data.repository.DnsGuardRepository
import com.privatednsguard.data.security.SecurityManager
import com.privatednsguard.service.PrivateDnsWatchdogService
import com.privatednsguard.ui.screens.AuditLogsScreen
import com.privatednsguard.ui.screens.DnsConfigScreen
import com.privatednsguard.ui.screens.HomeScreen
import com.privatednsguard.ui.screens.SecuritySettingsScreen
import com.privatednsguard.ui.screens.SetupWizardScreen
import com.privatednsguard.ui.theme.DarkBorder
import com.privatednsguard.ui.theme.DarkSurface
import com.privatednsguard.ui.theme.GuardGreen
import com.privatednsguard.ui.theme.TextPrimary
import com.privatednsguard.ui.theme.TextSecondary

enum class NavTab(val title: String, val icon: ImageVector) {
    HOME("Guard", Icons.Default.Shield),
    CONFIG("DNS Policy", Icons.Default.Dns),
    LOGS("Audit Logs", Icons.Default.History),
    SECURITY("Security", Icons.Default.Security)
}

@Composable
fun MainScreen(
    repository: DnsGuardRepository,
    securityManager: SecurityManager,
    context: android.content.Context
) {
    var selectedTab by remember { mutableStateOf(NavTab.HOME) }
    var isSetupWizardOpen by remember { mutableStateOf(false) }

    val policyState by repository.policyState.collectAsState()

    Scaffold(
        bottomBar = {
            if (!isSetupWizardOpen) {
                NavigationBar(
                    containerColor = DarkSurface,
                    tonalElevation = 3.dp
                ) {
                    NavTab.entries.forEach { tab ->
                        val selected = (selectedTab == tab)
                        NavigationBarItem(
                            selected = selected,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title
                                )
                            },
                            label = { Text(tab.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = DarkSurface,
                                selectedTextColor = GuardGreen,
                                indicatorColor = GuardGreen,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isSetupWizardOpen) {
                SetupWizardScreen(
                    state = policyState,
                    repository = repository,
                    onBack = { isSetupWizardOpen = false }
                )
            } else {
                when (selectedTab) {
                    NavTab.HOME -> {
                        HomeScreen(
                            state = policyState,
                            securityManager = securityManager,
                            onToggleGuard = { enabled ->
                                repository.setGuardEnabled(enabled)
                                if (enabled) {
                                    PrivateDnsWatchdogService.start(context)
                                } else {
                                    PrivateDnsWatchdogService.stop(context)
                                }
                            },
                            onAdoptCurrentSystemDns = {
                                repository.adoptCurrentSystemDns()
                            },
                            onSnoozeSelected = { minutes ->
                                repository.snoozeProtection(minutes)
                                PrivateDnsWatchdogService.update(context)
                            },
                            onCancelSnooze = {
                                repository.cancelSnooze()
                                PrivateDnsWatchdogService.update(context)
                            },
                            onNavigateToSetup = { isSetupWizardOpen = true },
                            onNavigateToLogs = { selectedTab = NavTab.LOGS },
                            onNavigateToConfig = { selectedTab = NavTab.CONFIG }
                        )
                    }
                    NavTab.CONFIG -> {
                        DnsConfigScreen(
                            state = policyState,
                            repository = repository
                        )
                    }
                    NavTab.LOGS -> {
                        AuditLogsScreen(
                            repository = repository
                        )
                    }
                    NavTab.SECURITY -> {
                        SecuritySettingsScreen(
                            state = policyState,
                            securityManager = securityManager,
                            onNavigateToSetup = { isSetupWizardOpen = true }
                        )
                    }
                }
            }
        }
    }
}
