package com.privatednsguard.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tamper_events")
data class TamperEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val attemptedMode: String,
    val attemptedSpecifier: String,
    val revertedMode: String,
    val revertedSpecifier: String,
    val reverted: Boolean = true,
    val message: String = "Tamper attempt detected and reverted."
)
