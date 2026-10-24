package com.privatednsguard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.privatednsguard.MainActivity
import com.privatednsguard.R
import com.privatednsguard.data.model.DnsMode
import com.privatednsguard.data.repository.DnsGuardRepository
import com.privatednsguard.data.security.SecurityManager
import com.privatednsguard.receiver.WatchdogRestartReceiver
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class PrivateDnsWatchdogService : Service() {

    private lateinit var repository: DnsGuardRepository
    private lateinit var securityManager: SecurityManager
    private lateinit var notificationManager: NotificationManager
    private var dnsObserver: ContentObserver? = null

    // SupervisorJob ensures failures in child coroutines do not kill the root scope
    private val serviceJob = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Watchdog coroutine error caught: ${throwable.message}", throwable)
    }
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob + exceptionHandler)

    private val enforcementMutex = Mutex()
    private var debounceJob: Job? = null
    private var watchdogPollingJob: Job? = null
    private var snoozeTickerJob: Job? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON,
                Intent.ACTION_USER_PRESENT -> {
                    // Check instantly when user turns phone on or unlocks (prime time for tampering)
                    schedulePolicyCheck(debounceMs = 0L)
                }
            }
        }
    }

    companion object {
        private const val TAG = "PrivateDnsWatchdog"

        const val CHANNEL_STATUS_ID = "dns_guard_watchdog_status"
        const val CHANNEL_ALERT_ID = "dns_guard_tamper_alert"
        const val NOTIFICATION_STATUS_ID = 2001
        const val NOTIFICATION_ALERT_ID = 2002

        const val ACTION_START = "com.privatednsguard.action.START"
        const val ACTION_STOP = "com.privatednsguard.action.STOP"
        const val ACTION_UPDATE = "com.privatednsguard.action.UPDATE"

        fun start(context: Context) {
            try {
                val intent = Intent(context, PrivateDnsWatchdogService::class.java).apply {
                    action = ACTION_START
                }
                ContextCompat.startForegroundService(context, intent)
                WatchdogRestartReceiver.schedulePeriodicAlarm(context)
            } catch (e: Throwable) {
                Log.w(TAG, "Failed startForegroundService (Android 12+ background limit): ${e.message}")
                WatchdogRestartReceiver.scheduleImmediateRestart(context, delayMs = 1500L)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, PrivateDnsWatchdogService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
                WatchdogRestartReceiver.cancelPeriodicAlarm(context)
            } catch (e: Throwable) {
                Log.w(TAG, "Failed stopService: ${e.message}")
            }
        }

        fun update(context: Context) {
            try {
                val intent = Intent(context, PrivateDnsWatchdogService::class.java).apply {
                    action = ACTION_UPDATE
                }
                context.startService(intent)
            } catch (e: Throwable) {
                Log.w(TAG, "Failed updateService: ${e.message}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "PrivateDnsWatchdogService onCreate")
        repository = DnsGuardRepository.getInstance(this)
        securityManager = SecurityManager(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannels()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            androidx.core.app.ServiceCompat.startForeground(
                this,
                NOTIFICATION_STATUS_ID,
                buildStatusNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_STATUS_ID, buildStatusNotification())
        }

        registerDnsObserver()
        registerScreenStateReceiver()
        startContinuousWatchdog()
        startSnoozeMonitoring()

        WatchdogRestartReceiver.schedulePeriodicAlarm(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE -> {
                notificationManager.notify(NOTIFICATION_STATUS_ID, buildStatusNotification())
                schedulePolicyCheck(debounceMs = 0L)
            }
            else -> {
                notificationManager.notify(NOTIFICATION_STATUS_ID, buildStatusNotification())
                schedulePolicyCheck(debounceMs = 0L)
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val state = repository.policyState.value
        if (state.isGuardEnabled) {
            Log.i(TAG, "User swiped app from Recents. Scheduling immediate watchdog restart.")
            WatchdogRestartReceiver.scheduleImmediateRestart(this, delayMs = 1000L)
        }
    }

    private fun registerScreenStateReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        try {
            registerReceiver(screenReceiver, filter)
        } catch (e: Throwable) {
            Log.w(TAG, "Could not register screenReceiver: ${e.message}")
        }
    }

    private fun registerDnsObserver() {
        if (dnsObserver != null) return

        val handler = Handler(Looper.getMainLooper())
        dnsObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                // Debounce rapid changes to prevent duplicate parallel executions
                schedulePolicyCheck(debounceMs = 200L)
            }
        }

        val modeUri = Settings.Global.getUriFor(DnsGuardRepository.SETTINGS_PRIVATE_DNS_MODE)
        val specifierUri = Settings.Global.getUriFor(DnsGuardRepository.SETTINGS_PRIVATE_DNS_SPECIFIER)

        try {
            contentResolver.registerContentObserver(modeUri, true, dnsObserver!!)
            contentResolver.registerContentObserver(specifierUri, true, dnsObserver!!)
            Log.d(TAG, "DnsObserver successfully registered on Settings.Global URIs")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to register content observer: ${e.message}", e)
        }
    }

    private fun schedulePolicyCheck(debounceMs: Long = 200L) {
        debounceJob?.cancel()
        debounceJob = serviceScope.launch {
            if (debounceMs > 0) {
                delay(debounceMs)
            }
            checkAndEnforcePolicy()
        }
    }

    private fun startContinuousWatchdog() {
        watchdogPollingJob?.cancel()
        watchdogPollingJob = serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    checkAndEnforcePolicy()
                } catch (e: Throwable) {
                    Log.e(TAG, "Watchdog heartbeat loop error: ${e.message}", e)
                }
                // 15-second heartbeat: zero battery impact, doesn't trip OS background killers,
                // while ContentObserver and ScreenOnReceiver provide instant 0ms response.
                delay(15000L)
            }
        }
    }

    private suspend fun checkAndEnforcePolicy() = withContext(Dispatchers.IO) {
        enforcementMutex.withLock {
            val state = repository.policyState.value

            // If protection is disabled or currently snoozed, do not revert
            if (!state.isGuardEnabled || repository.isSnoozed()) {
                return@withLock
            }

            val activeMode = repository.readActiveSystemMode()
            val activeSpecifier = repository.readActiveSystemSpecifier()

            val isTargetMismatch = (activeMode != state.targetMode) ||
                    (state.targetMode == DnsMode.HOSTNAME && !activeSpecifier.equals(state.targetSpecifier, ignoreCase = true))

            if (isTargetMismatch) {
                Log.w(TAG, "Unauthorized DNS mismatch detected! Active: mode=${activeMode.systemValue}, specifier=$activeSpecifier; Target: mode=${state.targetMode.systemValue}, specifier=${state.targetSpecifier}")
                val reverted = repository.revertToTargetPolicy(
                    attemptedMode = activeMode.systemValue,
                    attemptedSpecifier = activeSpecifier
                )
                if (reverted) {
                    Log.i(TAG, "Successfully restored DNS target policy.")
                    showTamperAlertNotification(
                        attempted = if (activeMode == DnsMode.HOSTNAME) activeSpecifier else activeMode.displayName,
                        restored = if (state.targetMode == DnsMode.HOSTNAME) state.targetSpecifier else state.targetMode.displayName
                    )
                    notificationManager.notify(NOTIFICATION_STATUS_ID, buildStatusNotification())
                } else {
                    Log.e(TAG, "Failed to revert DNS setting. Verify WRITE_SECURE_SETTINGS permission.")
                }
            }
        }
    }

    private fun startSnoozeMonitoring() {
        snoozeTickerJob?.cancel()
        snoozeTickerJob = serviceScope.launch {
            while (isActive) {
                delay(5000L)
                if (repository.isSnoozed()) {
                    repository.refreshState()
                    notificationManager.notify(NOTIFICATION_STATUS_ID, buildStatusNotification())
                } else if (repository.policyState.value.snoozeRemainingSeconds > 0) {
                    // Snooze just expired!
                    repository.refreshState()
                    checkAndEnforcePolicy()
                    notificationManager.notify(NOTIFICATION_STATUS_ID, buildStatusNotification())
                }
            }
        }
    }

    private fun buildStatusNotification(): Notification {
        val state = repository.policyState.value
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title: String
        val content: String

        if (repository.isSnoozed()) {
            val mins = (state.snoozeRemainingSeconds / 60) + 1
            title = "PrivateDNS Guard: Paused ($mins min left)"
            content = "Protection is temporarily snoozed. Tap to resume."
        } else if (state.isGuardEnabled) {
            title = "PrivateDNS Guard: Active & Locked"
            val targetDisplay = if (state.targetMode == DnsMode.HOSTNAME) {
                state.targetSpecifier
            } else {
                state.targetMode.displayName
            }
            content = "Guarding: $targetDisplay"
        } else {
            title = "PrivateDNS Guard: Disarmed"
            content = "DNS protection is turned off."
        }

        return NotificationCompat.Builder(this, CHANNEL_STATUS_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingOpen)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun showTamperAlertNotification(attempted: String, restored: String) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundEnabled = securityManager.isAlertSoundEnabled()
        val builder = NotificationCompat.Builder(this, CHANNEL_ALERT_ID)
            .setContentTitle("⚠️ DNS Tamper Attempt Blocked!")
            .setContentText("Attempted: $attempted -> Reverted to: $restored")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "An unauthorized change to Private DNS was detected.\n" +
                            "Attempted: $attempted\n" +
                            "PrivateDNS Guard immediately restored your secure DNS ($restored)."
                )
            )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingOpen)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)

        if (soundEnabled) {
            builder.setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
        }

        notificationManager.notify(NOTIFICATION_ALERT_ID, builder.build())
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val statusChannel = NotificationChannel(
                CHANNEL_STATUS_ID,
                "Watchdog Service Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows the ongoing protection status of PrivateDNS Guard"
                setShowBadge(false)
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ALERT_ID,
                "Tamper Security Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority alerts when an unauthorized DNS change is blocked"
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(statusChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    override fun onDestroy() {
        Log.i(TAG, "PrivateDnsWatchdogService onDestroy")
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Throwable) {
        }

        dnsObserver?.let {
            try {
                contentResolver.unregisterContentObserver(it)
            } catch (_: Throwable) {
            }
            dnsObserver = null
        }
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
