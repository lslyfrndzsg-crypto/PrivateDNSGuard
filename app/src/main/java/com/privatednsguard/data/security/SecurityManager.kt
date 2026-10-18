package com.privatednsguard.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

class SecurityManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "dns_guard_security"
        private const val KEY_PIN_HASH = "key_pin_hash"
        private const val KEY_PIN_SALT = "key_pin_salt"
        private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
        private const val KEY_ALERT_SOUND_ENABLED = "key_alert_sound_enabled"
        private const val KEY_FAILED_ATTEMPTS = "key_failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "key_lockout_until"
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 30_000L // 30 seconds
    }

    fun isPinSet(): Boolean {
        return prefs.getString(KEY_PIN_HASH, null) != null
    }

    fun setPin(pin: String) {
        val saltBytes = ByteArray(16)
        SecureRandom().nextBytes(saltBytes)
        val saltBase64 = Base64.encodeToString(saltBytes, Base64.NO_WRAP)
        val hash = hashPin(pin, saltBytes)

        prefs.edit()
            .putString(KEY_PIN_SALT, saltBase64)
            .putString(KEY_PIN_HASH, hash)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
    }

    fun removePin() {
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .remove(KEY_BIOMETRIC_ENABLED)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
    }

    fun verifyPin(pin: String): PinVerificationResult {
        val now = System.currentTimeMillis()
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        if (now < lockoutUntil) {
            val secondsRemaining = ((lockoutUntil - now) / 1000).coerceAtLeast(1)
            return PinVerificationResult.LockedOut(secondsRemaining)
        }

        val storedHash = prefs.getString(KEY_PIN_HASH, null)
        val storedSaltBase64 = prefs.getString(KEY_PIN_SALT, null)

        if (storedHash == null || storedSaltBase64 == null) {
            return PinVerificationResult.Success
        }

        val saltBytes = Base64.decode(storedSaltBase64, Base64.NO_WRAP)
        val computedHash = hashPin(pin, saltBytes)

        return if (storedHash == computedHash) {
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL, 0L)
                .apply()
            PinVerificationResult.Success
        } else {
            val failed = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
            if (failed >= MAX_FAILED_ATTEMPTS) {
                val lockoutTime = now + LOCKOUT_DURATION_MS
                prefs.edit()
                    .putInt(KEY_FAILED_ATTEMPTS, 0)
                    .putLong(KEY_LOCKOUT_UNTIL, lockoutTime)
                    .apply()
                PinVerificationResult.LockedOut(LOCKOUT_DURATION_MS / 1000)
            } else {
                prefs.edit().putInt(KEY_FAILED_ATTEMPTS, failed).apply()
                PinVerificationResult.Failed(attemptsRemaining = MAX_FAILED_ATTEMPTS - failed)
            }
        }
    }

    fun isBiometricSupported(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false) && isPinSet()
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isAlertSoundEnabled(): Boolean {
        return prefs.getBoolean(KEY_ALERT_SOUND_ENABLED, true)
    }

    fun setAlertSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ALERT_SOUND_ENABLED, enabled).apply()
    }

    private fun hashPin(pin: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        val hashBytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
    }
}

sealed class PinVerificationResult {
    data object Success : PinVerificationResult()
    data class Failed(val attemptsRemaining: Int) : PinVerificationResult()
    data class LockedOut(val secondsRemaining: Long) : PinVerificationResult()
}
