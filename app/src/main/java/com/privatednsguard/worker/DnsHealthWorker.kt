package com.privatednsguard.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.privatednsguard.data.repository.DnsGuardRepository
import com.privatednsguard.service.PrivateDnsWatchdogService
import java.util.concurrent.TimeUnit

class DnsHealthWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = DnsGuardRepository.getInstance(applicationContext)
        repository.refreshState()

        val state = repository.policyState.value
        if (state.isGuardEnabled && !repository.isSnoozed()) {
            // Ensure service is running
            PrivateDnsWatchdogService.start(applicationContext)

            // Verify system settings match policy
            val activeMode = repository.readActiveSystemMode()
            val activeSpecifier = repository.readActiveSystemSpecifier()

            val isMatch = (activeMode == state.targetMode) &&
                    (state.targetMode != com.privatednsguard.data.model.DnsMode.HOSTNAME ||
                            activeSpecifier.equals(state.targetSpecifier, ignoreCase = true))

            if (!isMatch) {
                repository.revertToTargetPolicy(
                    attemptedMode = activeMode.systemValue,
                    attemptedSpecifier = activeSpecifier
                )
            }
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "dns_health_checker_work"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder().build()
            val request = PeriodicWorkRequestBuilder<DnsHealthWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
