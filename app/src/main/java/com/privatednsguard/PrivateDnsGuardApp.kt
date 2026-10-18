package com.privatednsguard

import android.app.Application
import com.privatednsguard.data.repository.DnsGuardRepository
import com.privatednsguard.service.PrivateDnsWatchdogService
import com.privatednsguard.worker.DnsHealthWorker

class PrivateDnsGuardApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val repository = DnsGuardRepository.getInstance(this)
        repository.refreshState()

        // Initialize Shizuku binder listeners
        com.privatednsguard.shizuku.ShizukuManager.init {
            repository.refreshState()
        }

        // Schedule periodic health worker
        DnsHealthWorker.schedule(this)

        // If guard was enabled, ensure watchdog service is active
        if (repository.policyState.value.isGuardEnabled) {
            PrivateDnsWatchdogService.start(this)
        }
    }
}
