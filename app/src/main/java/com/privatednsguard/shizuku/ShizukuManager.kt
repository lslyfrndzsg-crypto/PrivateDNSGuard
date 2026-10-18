package com.privatednsguard.shizuku

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuManager {

    const val SHIZUKU_REQUEST_CODE = 1001

    fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isShizukuRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }
    }

    fun hasShizukuPermission(): Boolean {
        return try {
            if (isShizukuRunning()) {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        }
    }

    fun requestShizukuPermission() {
        try {
            if (isShizukuRunning()) {
                Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
            }
        } catch (_: Throwable) {
            // Ignored if binder error
        }
    }

    suspend fun grantWriteSecureSettings(packageName: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isShizukuRunning()) {
            return@withContext Result.failure(IllegalStateException("Shizuku service is not running. Please start Shizuku first."))
        }
        if (!hasShizukuPermission()) {
            return@withContext Result.failure(IllegalStateException("PrivateDNS Guard does not have Shizuku permission yet."))
        }

        try {
            val cmd = "pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"
            val process = Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val error = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                Result.success("Permission successfully granted via Shizuku!")
            } else {
                Result.failure(RuntimeException("Failed to grant permission (exit code $exitCode): $error $output"))
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }
}
