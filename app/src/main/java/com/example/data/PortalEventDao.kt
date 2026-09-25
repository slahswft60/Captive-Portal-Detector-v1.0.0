package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PortalEventDao {
    @Query("SELECT * FROM portal_events ORDER BY timestamp DESC LIMIT 100")
    fun getAllEvents(): Flow<List<PortalEvent>>

    @Query("SELECT * FROM portal_events WHERE eventType = 'PORTAL_DETECTED' ORDER BY timestamp DESC LIMIT 50")
    fun getDetectedPortals(): Flow<List<PortalEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: PortalEvent): Long

    @Query("DELETE FROM portal_events")
    suspend fun clearAll()

    @Query("DELETE FROM portal_events WHERE id NOT IN (SELECT id FROM portal_events ORDER BY timestamp DESC LIMIT 200)")
    suspend fun pruneOldEvents()
}
