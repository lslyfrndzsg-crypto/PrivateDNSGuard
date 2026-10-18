package com.privatednsguard.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TamperDao {

    @Query("SELECT * FROM tamper_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<TamperEvent>>

    @Query("SELECT COUNT(*) FROM tamper_events")
    fun getEventCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tamper_events")
    suspend fun getEventCountDirect(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: TamperEvent): Long

    @Query("DELETE FROM tamper_events")
    suspend fun clearAll()
}
