package com.privatednsguard.shizuku

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuManager {

    const val SHIZUKU_REQUEST_CODE = 1001

    private val _isBinderReceived = MutableStateFlow(false)
    val isBinderReceived: StateFlow<Boolean> = _isBinderReceived.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private var onStatusChangedListener: (() -> Unit)? = null

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        _isBinderReceived.value = true
        _hasPermission.value = checkPermissionDirect()
        onStatusChangedListener?.invoke()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _isBinderReceived.value = false
        _hasPermission.value = false
        onStatusChangedListener?.invoke()
    }

    private val requestPermissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == SHIZUKU_REQUEST_CODE) {
            _hasPermission.value = (grantResult == PackageManager.PERMISSION_GRANTED)
            onStatusChangedListener?.invoke()
        }
    }

    fun init(onStatusChanged: (() -> Unit)? = null) {
        onStatusChangedListener = onStatusChanged
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
            checkAndUpdate()
        } catch (_: Throwable) {
        }
    }

    fun checkAndUpdate() {
        val running = isShizukuRunning()
        _isBinderReceived.value = running
        _hasPermission.value = if (running) checkPermissionDirect() else false
        onStatusChangedListener?.invoke()
    }

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

    private fun checkPermissionDirect(): Boolean {
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

    fun hasShizukuPermission(): Boolean {
        return _hasPermission.value || checkPermissionDirect()
    }

    fun requestShizukuPermission() {
        try {
            if (isShizukuRunning()) {
                Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
            }
        } catch (_: Throwable) {
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
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).apply { isAccessible = true }
            val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", cmd), null, null) as java.lang.Process
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
