package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_reports")
data class ScanReport(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val scanType: String, // "Quick" or "Deep" or "Junk"
    val itemsScanned: Int,
    val malwareCount: Int,
    val detectedMalwareList: String, // Comma separated list of malware names
    val status: String // "SAFE", "THREAT_FOUND", "CLEANED"
)

@Entity(tableName = "blocked_attempts")
data class BlockedAttempt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appName: String,
    val packageName: String,
    val threatType: String, // "Adware", "Trojan", "Spyware", "Ransomware"
    val timestamp: Long = System.currentTimeMillis()
)
