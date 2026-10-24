package com.privatednsguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.privatednsguard.data.repository.DnsGuardRepository
import com.privatednsguard.service.PrivateDnsWatchdogService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                val repository = DnsGuardRepository.getInstance(context)
                repository.refreshState()

                if (repository.policyState.value.isGuardEnabled) {
                    // Start the watchdog service to resume monitoring & auto-revert
                    PrivateDnsWatchdogService.start(context)
                    WatchdogRestartReceiver.schedulePeriodicAlarm(context)
                }
            }
        }
    }
}
