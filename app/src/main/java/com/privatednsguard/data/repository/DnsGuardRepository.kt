package com.privatednsguard.data.repository

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.PowerManager
import android.provider.Settings
import com.privatednsguard.data.db.AppDatabase
import com.privatednsguard.data.db.TamperEvent
import com.privatednsguard.data.model.DnsMode
import com.privatednsguard.data.model.DnsPolicyState
import com.privatednsguard.receiver.DnsAdminReceiver
import com.privatednsguard.shizuku.ShizukuManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class DnsGuardRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val database = AppDatabase.getInstance(context)
    private val tamperDao = database.tamperDao()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val revertMutex = Mutex()

    private val _policyState = MutableStateFlow(DnsPolicyState())
    val policyState: StateFlow<DnsPolicyState> = _policyState.asStateFlow()

    companion object {
        private const val PREFS_NAME = "dns_guard_policy"
        private const val KEY_GUARD_ENABLED = "key_guard_enabled"
        private const val KEY_TARGET_MODE = "key_target_mode"
        private const val KEY_TARGET_SPECIFIER = "key_target_specifier"
        private const val KEY_SNOOZE_UNTIL = "key_snooze_until"

        const val SETTINGS_PRIVATE_DNS_MODE = "private_dns_mode"
        const val SETTINGS_PRIVATE_DNS_SPECIFIER = "private_dns_specifier"

        @Volatile
        private var INSTANCE: DnsGuardRepository? = null

        fun getInstance(context: Context): DnsGuardRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = DnsGuardRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    init {
        refreshState()
        scope.launch {
            tamperDao.getEventCount().collectLatest { count ->
                _policyState.update { it.copy(blockedTamperCount = count) }
            }
        }
    }

    fun hasSecureSettingsPermission(): Boolean {
        return context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun readActiveSystemMode(): DnsMode {
        return try {
            val modeStr = Settings.Global.getString(context.contentResolver, SETTINGS_PRIVATE_DNS_MODE)
            DnsMode.fromSystemValue(modeStr)
        } catch (_: Throwable) {
            DnsMode.OPPORTUNISTIC
        }
    }

    fun readActiveSystemSpecifier(): String {
        return try {
            Settings.Global.getString(context.contentResolver, SETTINGS_PRIVATE_DNS_SPECIFIER) ?: ""
        } catch (_: Throwable) {
            ""
        }
    }

    fun isDeviceAdminActive(): Boolean {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = ComponentName(context, DnsAdminReceiver::class.java)
            dpm.isAdminActive(component)
        } catch (_: Throwable) {
            false
        }
    }

    fun refreshState() {
        val hasPermission = hasSecureSettingsPermission()
        val isShizukuRunning = ShizukuManager.isShizukuRunning()
        val isShizukuGranted = ShizukuManager.hasShizukuPermission()
        val isDeviceAdmin = isDeviceAdminActive()

        val isGuardEnabled = prefs.getBoolean(KEY_GUARD_ENABLED, false)
        val targetModeStr = prefs.getString(KEY_TARGET_MODE, DnsMode.HOSTNAME.systemValue)
        val targetMode = DnsMode.fromSystemValue(targetModeStr)
        val targetSpecifier = prefs.getString(KEY_TARGET_SPECIFIER, "dns.nextdns.io") ?: "dns.nextdns.io"

        val activeMode = readActiveSystemMode()
        val activeSpecifier = readActiveSystemSpecifier()

        val snoozeUntil = prefs.getLong(KEY_SNOOZE_UNTIL, 0L)
        val now = System.currentTimeMillis()
        val remainingSnoozeSeconds = if (snoozeUntil > now) (snoozeUntil - now) / 1000 else 0L

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isBatteryOptimizationIgnored = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false

        _policyState.update {
            it.copy(
                isGuardEnabled = isGuardEnabled,
                targetMode = targetMode,
                targetSpecifier = targetSpecifier,
                activeMode = activeMode,
                activeSpecifier = activeSpecifier,
                hasSecureSettingsPermission = hasPermission,
                isShizukuAvailable = isShizukuRunning,
                isShizukuGranted = isShizukuGranted,
                isDeviceAdminActive = isDeviceAdmin,
                isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
                snoozeRemainingSeconds = remainingSnoozeSeconds
            )
        }
    }

    fun setGuardEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GUARD_ENABLED, enabled).apply()
        if (enabled) {
            // Cancel any active snooze when turning on
            cancelSnooze()
        }
        refreshState()
    }

    fun setTargetPolicy(mode: DnsMode, specifier: String) {
        prefs.edit()
            .putString(KEY_TARGET_MODE, mode.systemValue)
            .putString(KEY_TARGET_SPECIFIER, specifier.trim())
            .apply()
        refreshState()
    }

    /**
     * Reads whatever Private DNS setting is currently active in Android
     * (e.g. user manually configured NextDNS in Android Settings) and sets it as the locked policy!
     */
    fun adoptCurrentSystemDns(): Boolean {
        val currentMode = readActiveSystemMode()
        val currentSpecifier = readActiveSystemSpecifier()

        prefs.edit()
            .putString(KEY_TARGET_MODE, currentMode.systemValue)
            .putString(KEY_TARGET_SPECIFIER, currentSpecifier)
            .apply()

        refreshState()
        return true
    }

    suspend fun applyTargetDnsToSystem(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!hasSecureSettingsPermission()) {
            return@withContext Result.failure(SecurityException("WRITE_SECURE_SETTINGS permission is missing."))
        }

        val targetMode = policyState.value.targetMode
        val targetSpecifier = policyState.value.targetSpecifier

        try {
            Settings.Global.putString(
                context.contentResolver,
                SETTINGS_PRIVATE_DNS_MODE,
                targetMode.systemValue
            )
            if (targetMode == DnsMode.HOSTNAME) {
                Settings.Global.putString(
                    context.contentResolver,
                    SETTINGS_PRIVATE_DNS_SPECIFIER,
                    targetSpecifier
                )
            }
            refreshState()
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun revertToTargetPolicy(
        attemptedMode: String,
        attemptedSpecifier: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (!hasSecureSettingsPermission()) return@withContext false

        revertMutex.withLock {
            val targetMode = policyState.value.targetMode
            val targetSpecifier = policyState.value.targetSpecifier

            var success = false
            try {
                Settings.Global.putString(
                    context.contentResolver,
                    SETTINGS_PRIVATE_DNS_MODE,
                    targetMode.systemValue
                )
                if (targetMode == DnsMode.HOSTNAME) {
                    Settings.Global.putString(
                        context.contentResolver,
                        SETTINGS_PRIVATE_DNS_SPECIFIER,
                        targetSpecifier
                    )
                }
                success = true
            } catch (_: Throwable) {
                success = false
            }

            // Log the tamper incident to Room DB
            try {
                val event = TamperEvent(
                    attemptedMode = attemptedMode,
                    attemptedSpecifier = attemptedSpecifier,
                    revertedMode = targetMode.systemValue,
                    revertedSpecifier = targetSpecifier,
                    reverted = success,
                    message = if (success) {
                        "Blocked attempt to change DNS. Reverted to ${if (targetMode == DnsMode.HOSTNAME) targetSpecifier else targetMode.displayName}."
                    } else {
                        "Detected unauthorized DNS change but failed to write Settings (check permissions)."
                    }
                )
                tamperDao.insertEvent(event)
            } catch (_: Throwable) {
            }

            refreshState()
            success
        }
    }

    fun snoozeProtection(durationMinutes: Int) {
        val until = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        prefs.edit().putLong(KEY_SNOOZE_UNTIL, until).apply()
        refreshState()
    }

    fun cancelSnooze() {
        prefs.edit().putLong(KEY_SNOOZE_UNTIL, 0L).apply()
        refreshState()
    }

    fun isSnoozed(): Boolean {
        val until = prefs.getLong(KEY_SNOOZE_UNTIL, 0L)
        return until > System.currentTimeMillis()
    }

    suspend fun clearTamperLogs() = withContext(Dispatchers.IO) {
        tamperDao.clearAll()
    }

    fun getAllTamperEvents() = tamperDao.getAllEvents()
}
