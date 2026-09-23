package com.pkfuturegkgs.hardsecurityguard.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {
    @Insert
    suspend fun insert(entity: ScanHistoryEntity): Long

    @Query("SELECT * FROM scan_history ORDER BY id DESC")
    fun observeHistory(): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history WHERE synced = 0 ORDER BY id ASC")
    suspend fun getUnsynced(): List<ScanHistoryEntity>

    @Query("UPDATE scan_history SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("DELETE FROM scan_history")
    suspend fun deleteAll()
}
