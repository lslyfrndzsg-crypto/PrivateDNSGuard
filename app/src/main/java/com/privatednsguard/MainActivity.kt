package com.privatednsguard

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.privatednsguard.data.repository.DnsGuardRepository
import com.privatednsguard.data.security.SecurityManager
import com.privatednsguard.ui.MainScreen
import com.privatednsguard.ui.components.PinAuthDialog
import com.privatednsguard.ui.theme.DarkBackground
import com.privatednsguard.ui.theme.PrivateDNSGuardTheme

class MainActivity : FragmentActivity() {

    private lateinit var repository: DnsGuardRepository
    private lateinit var securityManager: SecurityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = DnsGuardRepository.getInstance(this)
        securityManager = SecurityManager(this)

        setContent {
            PrivateDNSGuardTheme {
                var isAppUnlocked by remember {
                    mutableStateOf(!securityManager.isPinSet())
                }

                if (!isAppUnlocked && securityManager.isPinSet()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkBackground)
                    ) {
                        PinAuthDialog(
                            title = "PrivateDNS Guard Locked",
                            subtitle = "Authenticate to access settings and protection controls",
                            securityManager = securityManager,
                            onSuccess = { isAppUnlocked = true },
                            onDismiss = { finish() },
                            onBiometricRequested = { triggerBiometricPrompt { isAppUnlocked = true } }
                        )
                    }
                } else {
                    MainScreen(
                        repository = repository,
                        securityManager = securityManager,
                        context = this
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        repository.refreshState()
        com.privatednsguard.shizuku.ShizukuManager.checkAndUpdate()
    }

    private fun triggerBiometricPrompt(onSuccess: () -> Unit) {
        if (!securityManager.isBiometricEnabled() || !securityManager.isBiometricSupported(this)) return

        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock PrivateDNS Guard")
            .setSubtitle("Confirm identity to access guard controls")
            .setNegativeButtonText("Use PIN")
            .build()

        prompt.authenticate(promptInfo)
    }
}
