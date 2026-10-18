package com.privatednsguard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.privatednsguard.MainActivity
import com.privatednsguard.R
import com.privatednsguard.data.model.DnsMode
import com.privatednsguard.data.repository.DnsGuardRepository
import com.privatednsguard.data.security.SecurityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PrivateDnsWatchdogService : Service() {

    private lateinit var repository: DnsGuardRepository
    private lateinit var securityManager: SecurityManager
    private lateinit var notificationManager: NotificationManager
    private var dnsObserver: ContentObserver? = null

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var snoozeTickerJob: Job? = null

    companion object {
        const val CHANNEL_STATUS_ID = "dns_guard_watchdog_status"
        const val CHANNEL_ALERT_ID = "dns_guard_tamper_alert"
        const val NOTIFICATION_STATUS_ID = 2001
        const val NOTIFICATION_ALERT_ID = 2002

        const val ACTION_START = "com.privatednsguard.action.START"
        const val ACTION_STOP = "com.privatednsguard.action.STOP"
        const val ACTION_UPDATE = "com.privatednsguard.action.UPDATE"

        fun start(context: Context) {
            val intent = Intent(context, PrivateDnsWatchdogService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, PrivateDnsWatchdogService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun update(context: Context) {
            val intent = Intent(context, PrivateDnsWatchdogService::class.java).apply {
                action = ACTION_UPDATE
            }
            context.startService(intent)
        }
    }

    private var watchdogPollingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
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
        startContinuousWatchdog()
        startSnoozeMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE -> {
                notificationManager.notify(NOTIFICATION_STATUS_ID, buildStatusNotification())
                checkAndEnforcePolicy()
            }
            else -> {
                notificationManager.notify(NOTIFICATION_STATUS_ID, buildStatusNotification())
                checkAndEnforcePolicy()
            }
        }
        return START_STICKY
    }

    private fun registerDnsObserver() {
        if (dnsObserver != null) return

        val handler = Handler(Looper.getMainLooper())
        dnsObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                checkAndEnforcePolicy()
            }

            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                checkAndEnforcePolicy()
            }
        }

        val modeUri = Settings.Global.getUriFor(DnsGuardRepository.SETTINGS_PRIVATE_DNS_MODE)
        val specifierUri = Settings.Global.getUriFor(DnsGuardRepository.SETTINGS_PRIVATE_DNS_SPECIFIER)

        try {
            contentResolver.registerContentObserver(modeUri, true, dnsObserver!!)
            contentResolver.registerContentObserver(specifierUri, true, dnsObserver!!)
        } catch (_: Throwable) {
        }
    }

    private fun startContinuousWatchdog() {
        watchdogPollingJob?.cancel()
        watchdogPollingJob = serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    checkAndEnforcePolicy()
                } catch (_: Throwable) {
                }
                delay(1000) // 1-second active heartbeat ensures auto-revert even if observer is throttled
            }
        }
    }

    private fun checkAndEnforcePolicy() {
        repository.refreshState()
        val state = repository.policyState.value

        // If protection is disabled or currently snoozed, do not revert
        if (!state.isGuardEnabled || repository.isSnoozed()) {
            return
        }

        val activeMode = repository.readActiveSystemMode()
        val activeSpecifier = repository.readActiveSystemSpecifier()

        val isTargetMismatch = (activeMode != state.targetMode) ||
                (state.targetMode == DnsMode.HOSTNAME && !activeSpecifier.equals(state.targetSpecifier, ignoreCase = true))

        if (isTargetMismatch) {
            // Unauthorized change detected! Immediately revert back!
            serviceScope.launch(Dispatchers.IO) {
                val reverted = repository.revertToTargetPolicy(
                    attemptedMode = activeMode.systemValue,
                    attemptedSpecifier = activeSpecifier
                )
                if (reverted) {
                    showTamperAlertNotification(
                        attempted = if (activeMode == DnsMode.HOSTNAME) activeSpecifier else activeMode.displayName,
                        restored = if (state.targetMode == DnsMode.HOSTNAME) state.targetSpecifier else state.targetMode.displayName
                    )
                    notificationManager.notify(NOTIFICATION_STATUS_ID, buildStatusNotification())
                }
            }
        }
    }

    private fun startSnoozeMonitoring() {
        snoozeTickerJob?.cancel()
        snoozeTickerJob = serviceScope.launch {
            while (isActive) {
                delay(5000)
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
        dnsObserver?.let {
            contentResolver.unregisterContentObserver(it)
            dnsObserver = null
        }
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
