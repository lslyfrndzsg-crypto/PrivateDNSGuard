package com.privatednsguard.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import com.privatednsguard.data.model.DnsMode
import com.privatednsguard.data.repository.DnsGuardRepository
import com.privatednsguard.service.PrivateDnsWatchdogService

/**
 * High-reliability BroadcastReceiver to resurrect and self-heal PrivateDnsWatchdogService
 * if it was terminated by Android OS, OEM task managers, or when the task is removed from Recents.
 *
 * Utilizes AlarmManager.setAndAllowWhileIdle to punch through Android Doze mode and
 * acquire a brief WakeLock to verify and revert DNS tampering immediately.
 */
class WatchdogRestartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d(TAG, "WatchdogRestartReceiver triggered with action: $action")

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PrivateDNSGuard:WatchdogRestartWakeLock"
        )?.apply {
            setReferenceCounted(false)
            acquire(3000L) // 3 seconds is plenty to verify and write Settings.Global
        }

        try {
            val repository = DnsGuardRepository.getInstance(context)
            val state = repository.policyState.value

            if (state.isGuardEnabled && !repository.isSnoozed()) {
                val activeMode = repository.readActiveSystemMode()
                val activeSpecifier = repository.readActiveSystemSpecifier()

                val isMismatch = (activeMode != state.targetMode) ||
                        (state.targetMode == DnsMode.HOSTNAME && !activeSpecifier.equals(state.targetSpecifier, ignoreCase = true))

                // Immediate revert right inside receiver in case service startup is throttled by Android 12+ background limits
                if (isMismatch && repository.hasSecureSettingsPermission()) {
                    try {
                        Settings.Global.putString(
                            context.contentResolver,
                            DnsGuardRepository.SETTINGS_PRIVATE_DNS_MODE,
                            state.targetMode.systemValue
                        )
                        if (state.targetMode == DnsMode.HOSTNAME) {
                            Settings.Global.putString(
                                context.contentResolver,
                                DnsGuardRepository.SETTINGS_PRIVATE_DNS_SPECIFIER,
                                state.targetSpecifier
                            )
                        }
                        Log.i(TAG, "Restored target DNS policy directly in WatchdogRestartReceiver")
                    } catch (e: Throwable) {
                        Log.e(TAG, "Failed direct restore: ${e.message}", e)
                    }
                }

                // Ensure the continuous watchdog service is alive
                PrivateDnsWatchdogService.start(context)

                // Reschedule next self-healing heartbeat alarm
                schedulePeriodicAlarm(context)
            }
        } finally {
            wakeLock?.let {
                if (it.isHeld) {
                    try {
                        it.release()
                    } catch (_: Throwable) {
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "WatchdogRestartReceiver"
        const val ACTION_RESTART_WATCHDOG = "com.privatednsguard.action.RESTART_WATCHDOG"
        const val ACTION_HEARTBEAT_ALARM = "com.privatednsguard.action.HEARTBEAT_ALARM"

        private const val REQUEST_CODE_RESTART = 1001
        private const val REQUEST_CODE_HEARTBEAT = 1002
        private const val HEARTBEAT_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes

        fun scheduleImmediateRestart(context: Context, delayMs: Long = 1000L) {
            val intent = Intent(context, WatchdogRestartReceiver::class.java).apply {
                action = ACTION_RESTART_WATCHDOG
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_RESTART,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val triggerAt = SystemClock.elapsedRealtime() + delayMs

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to schedule restart alarm: ${e.message}")
            }
        }

        fun schedulePeriodicAlarm(context: Context) {
            val intent = Intent(context, WatchdogRestartReceiver::class.java).apply {
                action = ACTION_HEARTBEAT_ALARM
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_HEARTBEAT,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val triggerAt = SystemClock.elapsedRealtime() + HEARTBEAT_INTERVAL_MS

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to schedule periodic heartbeat alarm: ${e.message}")
            }
        }

        fun cancelPeriodicAlarm(context: Context) {
            val intent = Intent(context, WatchdogRestartReceiver::class.java).apply {
                action = ACTION_HEARTBEAT_ALARM
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_HEARTBEAT,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                alarmManager?.cancel(pendingIntent)
            }
        }
    }
}
