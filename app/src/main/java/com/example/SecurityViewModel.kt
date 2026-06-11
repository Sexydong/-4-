package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BlockedAttempt
import com.example.data.ScanReport
import com.example.data.SecurityDatabase
import com.example.data.SecurityRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

// Sealed class for Virus Scan states
sealed class VirusScanState {
    object Idle : VirusScanState()
    data class Scanning(val progress: Float, val currentAppName: String, val scannedCount: Int) : VirusScanState()
    data class ThreatsFound(val scannedCount: Int, val threats: List<String>) : VirusScanState()
    data class Safe(val scannedCount: Int) : VirusScanState()
    data class Cleaning(val progress: Float, val currentCleaningApp: String) : VirusScanState()
    object CleanSuccess : VirusScanState()
}

// Sealed class for Junk Clean states
sealed class JunkCleanState {
    object Idle : JunkCleanState()
    data class Scanning(val progress: Float, val currentFolder: String) : JunkCleanState()
    data class ReadyToClean(val junkSizeGb: Double) : JunkCleanState()
    data class Cleaning(val progress: Float) : JunkCleanState()
    data class Cleaned(val freedGb: Double) : JunkCleanState()
}

class SecurityViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val securityDao = SecurityDatabase.getDatabase(context).securityDao()
    private val repository = SecurityRepository(securityDao)

    // Flow for Room Tables
    val scanReports: StateFlow<List<ScanReport>> = repository.allScanReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedAttempts: StateFlow<List<BlockedAttempt>> = repository.allBlockedAttempts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI States
    val scanState = MutableStateFlow<VirusScanState>(VirusScanState.Idle)
    val junkState = MutableStateFlow<JunkCleanState>(JunkCleanState.ReadyToClean(1.24))

    // Real-time protection states
    val isLiveShieldActive = MutableStateFlow(true)
    val totalAppsMonitored = MutableStateFlow(124)
    val threatDatabaseVersion = MutableStateFlow("SG-2026.06.11A")

    // Active threats found in the latest scan (non-persistent unless scanned)
    val activeThreats = MutableStateFlow<List<String>>(listOf(
        "Trojan.Android.Agent.b",
        "Spyware.Keylogger.ShieldX",
        "Adware.PopApp.Free"
    ))

    // Recent simulation alerts
    val currentAlertMessage = MutableStateFlow<String?>(null)

    // Notification Channel ID
    private val CHANNEL_ID = "shieldguard_alerts"

    init {
        createNotificationChannel()
        // Update package count based on actual device count
        viewModelScope.launch {
            try {
                val pm = context.packageManager
                val packages = pm.getInstalledPackages(0)
                totalAppsMonitored.value = packages.size.coerceAtLeast(35)
            } catch (e: Exception) {
                totalAppsMonitored.value = 48
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "ShieldGuard 알림"
            val descriptionText = "실시간 바이러스 감지 및 실시간 가드 보호 이벤트"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendSystemNotification(title: String, message: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(Random.nextInt(1000), builder.build())
    }

    // Scan Simulation
    fun startVirusScan(isDeep: Boolean) {
        viewModelScope.launch {
            scanState.value = VirusScanState.Scanning(0f, "엔진 초기화 중...", 0)
            delay(800)

            val testApps = listOf(
                "com.android.chrome",
                "com.google.android.youtube",
                "com.google.android.apps.maps",
                "com.instagram.android",
                "com.facebook.katana",
                "com.spotify.music",
                "com.whatsapp",
                "com.netflix.mediaclient",
                "com.amazon.mShop.android.shopping",
                "com.example.dangerous.app",
                "com.cleaner.booster.ads",
                "com.secret.keylogger.system",
                "com.android.settings",
                "com.google.android.gms"
            )

            // Gather real package names if available to augment experience
            var realPackages = emptyList<String>()
            try {
                val pm = context.packageManager
                realPackages = pm.getInstalledPackages(0).map { it.packageName }
            } catch (e: Exception) { /* fallbacks */ }

            val appsToScan = if (realPackages.isNotEmpty()) {
                (realPackages + testApps).shuffled().take(if (isDeep) 60 else 25)
            } else {
                testApps.shuffled().take(if (isDeep) 14 else 8)
            }

            val totalSteps = appsToScan.size
            for (i in 0 until totalSteps) {
                val progress = (i + 1).toFloat() / totalSteps
                val app = appsToScan[i]
                scanState.value = VirusScanState.Scanning(progress, app, i + 1)
                delay(if (isDeep) 80L else 50L) // Scan effect
            }

            // Finish scan
            val currentThreatList = activeThreats.value
            if (currentThreatList.isNotEmpty()) {
                scanState.value = VirusScanState.ThreatsFound(totalSteps, currentThreatList)
                // Save Scan Report to Database
                repository.insertScanReport(
                    ScanReport(
                        scanType = if (isDeep) "정밀 검사" else "빠른 검사",
                        itemsScanned = totalSteps,
                        malwareCount = currentThreatList.size,
                        detectedMalwareList = currentThreatList.joinToString(", "),
                        status = "THREAT_FOUND"
                    )
                )
                // Alert User
                sendSystemNotification(
                    "⚠️ 악성 위협 감지됨!",
                    "ShieldGuard가 휴대전화에서 ${currentThreatList.size}개의 위험한 앱 위협을 감지했습니다."
                )
                currentAlertMessage.value = "⚠️ 위협 감지: ${currentThreatList.size}개의 바이러스 앱 발견!"
            } else {
                scanState.value = VirusScanState.Safe(totalSteps)
                // Save report
                repository.insertScanReport(
                    ScanReport(
                        scanType = if (isDeep) "정밀 검사" else "빠른 검사",
                        itemsScanned = totalSteps,
                        malwareCount = 0,
                        detectedMalwareList = "",
                        status = "SAFE"
                    )
                )
                sendSystemNotification(
                    "🛡️ 휴대전화 안전함",
                    "검사가 성공적으로 완료되었습니다. 발견된 위협 요소가 없습니다."
                )
            }
        }
    }

    // Auto delete and Clean threat action
    fun cleanDetectedThreats(onComplete: () -> Unit = {}) {
        val currentThreats = activeThreats.value
        if (currentThreats.isEmpty()) return

        viewModelScope.launch {
            val totalThreats = currentThreats.size
            for (i in 0 until totalThreats) {
                val threat = currentThreats[i]
                val progress = (i + 1).toFloat() / totalThreats
                scanState.value = VirusScanState.Cleaning(progress, "삭제 중: $threat")
                delay(1000) // Simulating deleting and system inspection log deletion
            }

            // Clear state and database logging
            activeThreats.value = emptyList()
            scanState.value = VirusScanState.CleanSuccess

            // Look up latest report in the database, update its status
            // For simplicity, we just insert a resolved report
            repository.insertScanReport(
                ScanReport(
                    scanType = "보안 치료",
                    itemsScanned = 0,
                    malwareCount = 0,
                    detectedMalwareList = "${currentThreats.joinToString(", ")} 해결됨",
                    status = "CLEANED"
                )
            )

            sendSystemNotification(
                "🛡️ 시스템 치료 완료",
                "모든 ${totalThreats}개의 위험한 바이러스 구성 요소가 성공적으로 삭제되었습니다."
            )
            currentAlertMessage.value = "🛡️ 보안 문제 해결: ${totalThreats}개의 위협 요소를 성공적으로 제거했습니다!"
            delay(1500)
            scanState.value = VirusScanState.Idle
            onComplete()
        }
    }

    // Block Installation Sim
    fun simulateAppInstallationBlock() {
        if (!isLiveShieldActive.value) {
            currentAlertMessage.value = "⚠️ 시뮬레이션 실패: 먼저 실시간 가드 보호(Live Shield)를 활성화하세요."
            return
        }

        viewModelScope.launch {
            val badApps = listOf(
                Pair("CryptoMiner Go", "com.crypto.miner.booster"),
                Pair("KeyGrep Tracker", "com.credential.grepper.logger"),
                Pair("System Premium Cleaner", "com.utility.cleaner.fake.trojan"),
                Pair("InstaView Hacker", "com.social.viewer.spy")
            ).shuffled()

            val selectedApp = badApps.first()

            // Save blocked attempt to DB
            val threatType = listOf("트로이 목마", "애드웨어", "스파이웨어", "랜섬웨어").shuffled().first()
            repository.insertBlockedAttempt(
                BlockedAttempt(
                    appName = selectedApp.first,
                    packageName = selectedApp.second,
                    threatType = threatType
                )
            )

            // Alert user
            val warningMsg = "악성 앱 설치 시도를 보호 차단했습니다: '${selectedApp.first}' (${selectedApp.second}) [위협 분류: $threatType]"
            currentAlertMessage.value = "🚫 $warningMsg"
            sendSystemNotification("🚫 설치 차단됨", warningMsg)
        }
    }

    // Junk Cleaner Sim
    fun startJunkScan() {
        viewModelScope.launch {
            junkState.value = JunkCleanState.Scanning(0f, "캐시 경로 확인 중...")
            val folders = listOf(
                "/data/user/0/cache_logs (로그 캐시)",
                "/storage/emulated/0/Android/obb/.residual (잔여 디렉토리)",
                "/sys/kernel/debug/tracing/logs_temp (임시 트레이싱 데이터)",
                "/storage/emulated/0/Download/obsolete_apks (사용되지 않는 APK)",
                "System Dalvik temporary cache (시스템 가상 메모리 캐시)"
            )

            for (i in folders.indices) {
                val progress = (i + 1).toFloat() / folders.size
                junkState.value = JunkCleanState.Scanning(progress, folders[i])
                delay(600)
            }

            junkState.value = JunkCleanState.ReadyToClean(1.24)
        }
    }

    fun cleanJunk() {
        viewModelScope.launch {
            junkState.value = JunkCleanState.Cleaning(0f)
            for (i in 1..10) {
                junkState.value = JunkCleanState.Cleaning(i / 10f)
                delay(200)
            }
            junkState.value = JunkCleanState.Cleaned(1.24)
            sendSystemNotification("⚡ 저장 공간 최적화", "정크 파일 및 불필요한 시스템 캐시 1.24 GB를 정상 완료했습니다!")
            delay(3000)
            junkState.value = JunkCleanState.Idle
        }
    }

    // Clear Alert Panel
    fun clearAlertMessage() {
        currentAlertMessage.value = null
    }

    fun toggleLiveShield() {
         isLiveShieldActive.value = !isLiveShieldActive.value
         val status = if (isLiveShieldActive.value) "활성화" else "비활성화"
         sendSystemNotification("🛡️ 실시간 감시 알림", "비공식 설치 차단 및 바이러스 실시간 감시가 ${status}되었습니다.")
    }

    fun resetAppMalware() {
        activeThreats.value = listOf(
            "Trojan.Android.Agent.b",
            "Spyware.Keylogger.ShieldX",
            "Adware.PopApp.Free"
        )
        scanState.value = VirusScanState.Idle
        currentAlertMessage.value = "🎒 정밀 테스트를 위해 가상의 위협 요소를 휴대폰 시스템 샌드박스에 원래대로 재배치했습니다."
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllReports()
            repository.clearAllBlocked()
            currentAlertMessage.value = "🧹 모든 검사 및 실시간 보안 차단 기록이 성공적으로 삭제되었습니다."
        }
    }
}
