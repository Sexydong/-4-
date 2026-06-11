package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SecurityDao {
    @Query("SELECT * FROM scan_reports ORDER BY timestamp DESC")
    fun getAllScanReports(): Flow<List<ScanReport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanReport(report: ScanReport): Long

    @Query("DELETE FROM scan_reports")
    suspend fun clearScanReports()

    @Query("UPDATE scan_reports SET status = :newStatus, malwareCount = 0, detectedMalwareList = '' WHERE id = :reportId")
    suspend fun cleanScanReport(reportId: Long, newStatus: String = "CLEANED")

    @Query("SELECT * FROM blocked_attempts ORDER BY timestamp DESC")
    fun getAllBlockedAttempts(): Flow<List<BlockedAttempt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedAttempt(attempt: BlockedAttempt)

    @Query("DELETE FROM blocked_attempts")
    suspend fun clearBlockedAttempts()
}
