package com.example.data

import kotlinx.coroutines.flow.Flow

class SecurityRepository(private val securityDao: SecurityDao) {
    val allScanReports: Flow<List<ScanReport>> = securityDao.getAllScanReports()
    val allBlockedAttempts: Flow<List<BlockedAttempt>> = securityDao.getAllBlockedAttempts()

    suspend fun insertScanReport(report: ScanReport): Long {
        return securityDao.insertScanReport(report)
    }

    suspend fun cleanScanReport(reportId: Long) {
        securityDao.cleanScanReport(reportId)
    }

    suspend fun clearAllReports() {
        securityDao.clearScanReports()
    }

    suspend fun insertBlockedAttempt(attempt: BlockedAttempt) {
        securityDao.insertBlockedAttempt(attempt)
    }

    suspend fun clearAllBlocked() {
        securityDao.clearBlockedAttempts()
    }
}
