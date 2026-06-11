package com.example

import android.Manifest
import android.app.Application
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.BlockedAttempt
import com.example.data.ScanReport
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.*

enum class ShieldTab {
    DASHBOARD,
    SHIELD,
    CLEANER,
    REPORTS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Request Notification Permission on Startup for Android 13+
                val context = LocalContext.current
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { _ -> }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val viewModel: SecurityViewModel = viewModel()
                SecurityAppMainScreen(viewModel)
            }
        }
    }
}

@Composable
fun SecurityAppMainScreen(viewModel: SecurityViewModel) {
    var activeTab by remember { mutableStateOf(ShieldTab.DASHBOARD) }
    
    MeshBackground {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent, // Let the mesh background shine through!
            bottomBar = {
                CustomBottomNavigationBar(
                    selectedTab = activeTab,
                    onTabSelected = { activeTab = it }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                // Top AppBar header
                headerAppBar()

                // Animated Alert Notification Overlay
                val alertMsg by viewModel.currentAlertMessage.collectAsState()
                AnimatedVisibility(visible = alertMsg != null) {
                    AlertNotificationBanner(
                        message = alertMsg ?: "",
                        onDismiss = { viewModel.clearAlertMessage() }
                    )
                }

                // Main Tab Content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    when (activeTab) {
                        ShieldTab.DASHBOARD -> DashboardScreen(
                            viewModel = viewModel,
                            onExploreCleaner = { activeTab = ShieldTab.CLEANER },
                            onExploreReports = { activeTab = ShieldTab.REPORTS }
                        )
                        ShieldTab.SHIELD -> AppBlockerScreen(viewModel = viewModel)
                        ShieldTab.CLEANER -> StorageCleanerScreen(viewModel = viewModel)
                        ShieldTab.REPORTS -> SecurityLogsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun ColumnScope.headerAppBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "SHIELDGUARD AI",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4F46E5),
                letterSpacing = 2.sp
            )
            Text(
                text = "보안 센터",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            )
        }
        
        // Pfp Avatar Container
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0x99FFFFFF))
                .border(1.dp, Color(0x7Fffffff), CircleShape)
                .clickable { }
                .padding(3.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(0xFF4F46E5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "User Avatar",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun MeshBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE8EDF2))
            .drawBehind {
                // Top-Left Indigo Radial Glow (Frosted Glass specification)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x66818CF8), // indigo-300, 40% open
                            Color(0x00818CF8)
                        ),
                        center = androidx.compose.ui.geometry.Offset(-size.width * 0.1f, -size.height * 0.1f),
                        radius = size.width * 0.7f
                    )
                )

                // Bottom-Right Blue Radial Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x5593C5FD), // blue-300, 30% open
                            Color(0x0093C5FD)
                        ),
                        center = androidx.compose.ui.geometry.Offset(size.width * 1.1f, size.height * 0.9f),
                        radius = size.width * 0.8f
                    )
                )

                // Mid-Left Purple Radial Glow for extra depth
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x2BD8B4F8), // purple-400
                            Color(0x00D8B4F8)
                        ),
                        center = androidx.compose.ui.geometry.Offset(-size.width * 0.3f, size.height * 0.5f),
                        radius = size.width * 0.6f
                    )
                )
            }
    ) {
        content()
    }
}

@Composable
fun FrostedGlassPanel(
    modifier: Modifier = Modifier,
    borderStroke: BorderStroke = BorderStroke(1.dp, Color(0x8CFFFFFF)),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        border = borderStroke,
        colors = CardDefaults.cardColors(
            containerColor = Color(0x54FFFFFF) // Frosted level translucency
        ),
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(28.dp),
                clip = false,
                ambientColor = Color(0x13000000),
                spotColor = Color(0x13000000)
            )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

@Composable
fun CustomBottomNavigationBar(
    selectedTab: ShieldTab,
    onTabSelected: (ShieldTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding() // CRITICAL: Avoid gesture pill overlap at the bottom
            .height(80.dp)
            .background(Color(0xD9FFFFFF)) // Translucent white bar
            .border(width = 1.dp, color = Color(0x1FFFFFFF))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavBarItem(
                tabName = "홈",
                icon = Icons.Default.Home,
                isSelected = selectedTab == ShieldTab.DASHBOARD,
                onClick = { onTabSelected(ShieldTab.DASHBOARD) },
                testTag = "nav_home"
            )
            NavBarItem(
                tabName = "실시간 보호",
                icon = Icons.Default.Lock,
                isSelected = selectedTab == ShieldTab.SHIELD,
                onClick = { onTabSelected(ShieldTab.SHIELD) },
                testTag = "nav_shield"
            )
            NavBarItem(
                tabName = "공간 정리",
                icon = Icons.Default.Delete,
                isSelected = selectedTab == ShieldTab.CLEANER,
                onClick = { onTabSelected(ShieldTab.CLEANER) },
                testTag = "nav_cleaner"
            )
            NavBarItem(
                tabName = "보안 보고서",
                icon = Icons.Default.List,
                isSelected = selectedTab == ShieldTab.REPORTS,
                onClick = { onTabSelected(ShieldTab.REPORTS) },
                testTag = "nav_reports"
            )
        }
    }
}

@Composable
fun NavBarItem(
    tabName: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val tint = if (isSelected) Color(0xFF4F46E5) else Color(0xFF94A3B8)
    val weight = if (isSelected) FontWeight.Bold else FontWeight.Medium
    
    Column(
        modifier = Modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .minimumInteractiveComponentSize(), // Ensure touch targets meet 48dp+
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tabName,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tabName.uppercase(),
            fontSize = 10.sp,
            fontWeight = weight,
            color = tint,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun AlertNotificationBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF312E81)), // Dark indigo alert background from HTML
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x33FFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Alert",
                        tint = Color(0xFFA5B4FC),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "실시간 가드 보안 알림",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC7D2FE),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = message,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings, // fallback Close style settings
                    contentDescription = "닫기",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ------------------------------------------------------------------------
// TAB 1: DASHBOARD
// ------------------------------------------------------------------------

@Composable
fun DashboardScreen(
    viewModel: SecurityViewModel,
    onExploreCleaner: () -> Unit,
    onExploreReports: () -> Unit
) {
    val scanState by viewModel.scanState.collectAsState()
    val activeThreatsList by viewModel.activeThreats.collectAsState()
    val liveShieldActive by viewModel.isLiveShieldActive.collectAsState()
    val blockedLogs by viewModel.blockedAttempts.collectAsState()
    val scanInProgress = scanState is VirusScanState.Scanning || scanState is VirusScanState.Cleaning

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Scan Status Card
        FrostedGlassPanel(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Interactive Circular Progress Area
                val scanRotation = remember { Animatable(0f) }
                
                // Spin rotation animation when scanning
                LaunchedEffect(scanState) {
                    if (scanState is VirusScanState.Scanning || scanState is VirusScanState.Cleaning) {
                        scanRotation.animateTo(
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            )
                        )
                    } else {
                        scanRotation.snapTo(0f)
                    }
                }

                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .padding(bottom = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer Ring
                    val ringColor = when (scanState) {
                        is VirusScanState.ThreatsFound -> Color(0xFFEF4444)
                        is VirusScanState.Safe -> Color(0xFF10B981)
                        is VirusScanState.Cleaning -> Color(0xFFF59E0B)
                        else -> Color(0xFFE2E8F0)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                drawCircle(
                                    color = ringColor.copy(alpha = 0.15f),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8.dp.toPx())
                                )
                            }
                    )

                    // Spinning sweep progress arc
                    if (scanState is VirusScanState.Scanning || scanState is VirusScanState.Cleaning) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .drawBehind {
                                    drawArc(
                                        color = Color(0xFF4F46E5),
                                        startAngle = scanRotation.value,
                                        sweepAngle = 100f,
                                        useCenter = false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = 8.dp.toPx(),
                                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                                        )
                                    )
                                }
                        )
                    }

                    // Inner Circle Pulse
                    val shieldIcon = when (scanState) {
                        is VirusScanState.ThreatsFound -> Icons.Default.Warning
                        is VirusScanState.Safe -> Icons.Default.Check
                        else -> Icons.Default.Lock
                    }
                    val shieldColor = when {
                        activeThreatsList.isNotEmpty() -> Color(0xFFEF4444)
                        else -> Color(0xFF4F46E5)
                    }

                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(shieldColor, shieldColor.copy(alpha = 0.8f))
                                )
                            )
                            .shadow(
                                elevation = 6.dp, 
                                shape = CircleShape,
                                ambientColor = shieldColor, 
                                spotColor = shieldColor
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = shieldIcon,
                            contentDescription = "Shield Guard",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                // Dynamic Status Texts
                when (val state = scanState) {
                    is VirusScanState.Idle -> {
                        if (activeThreatsList.isNotEmpty()) {
                            Text(
                                text = "⚠️ 장치 장벽 약화됨",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                            Text(
                                text = "${activeThreatsList.size}개의 치명적인 앱 위협이 남아 있습니다",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(top = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "상태: 최고 보안 가드 작 작동 중",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "마지막 보안 검사: 2시간 전",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    is VirusScanState.Scanning -> {
                        Text(
                            text = "휴대폰 정밀 탐색 중...",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "검사 중인 항목 (${state.scannedCount}개 완료): ${state.currentAppName}",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF4F46E5),
                            trackColor = Color(0x1F000000)
                        )
                    }
                    is VirusScanState.ThreatsFound -> {
                        Text(
                            text = "⚠️ 보안 실감 위험 감지!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                        Text(
                            text = "보안 비상: ${state.threats.size}개의 악성 앱들이 발견되었습니다.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(top = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    is VirusScanState.Safe -> {
                        Text(
                            text = "✅ 휴대폰 검사 완료 (안전)",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                        Text(
                            text = "성공적으로 ${state.scannedCount}개 노드를 검사했습니다. 위협 발견 없음.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(top = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    is VirusScanState.Cleaning -> {
                        Text(
                            text = "악성 앱 구성원 완전 치료 중...",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF59E0B)
                        )
                        Text(
                            text = state.currentCleaningApp,
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFFF59E0B),
                            trackColor = Color(0x1F000000)
                        )
                    }
                    is VirusScanState.CleanSuccess -> {
                        Text(
                            text = "✨ 보안 정화 완료",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                        Text(
                            text = "모든 의심스러운 전염 앱들이 깔끔하게 격리 및 삭제되었습니다.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(top = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Call to Action Button
                if (activeThreatsList.isNotEmpty() && (scanState is VirusScanState.Idle || scanState is VirusScanState.ThreatsFound)) {
                    Button(
                        onClick = { viewModel.cleanDetectedThreats() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("action_clean_threats")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "치료")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "감지된 악성 바이러스 앱 치료하기", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.startVirusScan(isDeep = true) },
                            enabled = !scanInProgress,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("action_deep_scan")
                        ) {
                            Text(text = "정밀 검사", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.startVirusScan(isDeep = false) },
                            enabled = !scanInProgress,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFF4F46E5)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4F46E5)),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("action_quick_scan")
                        ) {
                            Text(text = "빠른 검사", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Active Threat List Panel (if threats exist)
        if (activeThreatsList.isNotEmpty() && !scanInProgress) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x3EEF4444), RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0x66EF4444), RoundedCornerShape(24.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "⚠️ 즉각 치료가 필요한 악성 앱 탐지",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB91C1C)
                )
                Spacer(modifier = Modifier.height(8.dp))
                activeThreatsList.forEach { threat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = threat,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF7F1D1D)
                        )
                    }
                }
            }
        }

        // Feature Quad Grid (Exactly like Design HTML)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live Shield Box
            GridIndicatorCard(
                title = "실시간 감시",
                status = if (liveShieldActive) "실시간 보호 중" else "보호 중지됨",
                statusColor = if (liveShieldActive) Color(0xFF10B981) else Color(0xFF94A3B8),
                badgeText = if (liveShieldActive) "보호" else "비활성",
                icon = Icons.Default.CheckCircle,
                accentColor = Color(0xFFE6F4EA),
                tintColor = Color(0xFF10B981),
                onClick = { viewModel.toggleLiveShield() },
                modifier = Modifier.weight(1f)
            )

            // App Blocker Box
            GridIndicatorCard(
                title = "설치 차단",
                status = "${blockedLogs.size} 개 차단됨",
                statusColor = Color(0xFF3B82F6),
                badgeText = "실시간",
                icon = Icons.Default.Lock,
                accentColor = Color(0xFFE8F0FE),
                tintColor = Color(0xFF3B82F6),
                onClick = {  }, // Navigate manually or trigger state
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Junk Cleaner Box
            GridIndicatorCard(
                title = "공간 정리",
                status = "1.2 GB 정리 대기",
                statusColor = Color(0xFFF59E0B),
                badgeText = "비우기",
                icon = Icons.Default.Delete,
                accentColor = Color(0xFFFEF3C7),
                tintColor = Color(0xFFF59E0B),
                onClick = onExploreCleaner,
                modifier = Modifier.weight(1f)
            )

            // Reports Box
            GridIndicatorCard(
                title = "보안 보고서",
                status = "기록 최신화",
                statusColor = Color(0xFF8B5CF6),
                badgeText = "이력",
                icon = Icons.Default.List,
                accentColor = Color(0xFFF5F3FF),
                tintColor = Color(0xFF8B5CF6),
                onClick = onExploreReports,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
fun GridIndicatorCard(
    title: String,
    status: String,
    statusColor: Color,
    badgeText: String,
    icon: ImageVector,
    accentColor: Color,
    tintColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x8CFFFFFF)),
        border = BorderStroke(1.dp, Color(0x33FFFFFF)),
        modifier = modifier
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tintColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    Text(
                        text = badgeText.uppercase(),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------------
// TAB 2: LIVE SHIELD & APP BLOCKER (INTRUSION PREVENTION)
// ------------------------------------------------------------------------

@Composable
fun AppBlockerScreen(viewModel: SecurityViewModel) {
    val liveShieldActive by viewModel.isLiveShieldActive.collectAsState()
    val blockedLogs by viewModel.blockedAttempts.collectAsState()
    val totalMonitored by viewModel.totalAppsMonitored.collectAsState()
    val threatDatabase by viewModel.threatDatabaseVersion.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Blocker Dashboard Status
        FrostedGlassPanel(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "침입 예방 시스템 (IPS)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "장치 내 외부 설치 파일 및 웹 다운로드 인터페이스를 실시간으로 탐지합니다.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "실시간 감시 중인 시스템 노드: ${totalMonitored}개 앱",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4F46E5)
                    )
                    Text(
                        text = "보안 가드 데이터베이스 버전: ${threatDatabase}",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                }

                // High fidelity Switch layout
                Switch(
                    checked = liveShieldActive,
                    onCheckedChange = { viewModel.toggleLiveShield() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF10B981)
                    ),
                    modifier = Modifier.testTag("toggle_live_shield")
                )
            }
        }

        // Intrusion Sandbox threat generator simulator
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xCCFFFFFF)),
            border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "🛠️ 실시간 침입 방지 테스트 시뮬레이터",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D4ED8),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ShieldGuard의 강력한 실시간 감어 보호벽을 확인해보세요. 아래 버튼을 선택하면 위험한 비공식 악성 앱 설치 유도가 가상 재현됩니다. 실시간 감시 엔진이 즉각 침입을 차단하고 격리 로그에 저장합니다.",
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { viewModel.simulateAppInstallationBlock() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("action_simulate_block")
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = "시뮬레이션")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "가상 침입 설치 테스트 시도", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }

        // Block History Log Header
        Text(
            text = "실시간 감시 차단 이력 (${blockedLogs.size}개)",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        // Blocked attempts logs list
        if (blockedLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 30.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "안전",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "탐지된 외부 침입 시도가 없으며 완벽한 보안 상태입니다",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                blockedLogs.forEach { log ->
                    BlockedLogItemRow(log)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun BlockedLogItemRow(log: BlockedAttempt) {
    val dateText = remember(log.timestamp) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sdf.format(Date(log.timestamp))
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x66FFFFFF)),
        border = BorderStroke(1.dp, Color(0x54FFFFFF)),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(18.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFEE2E2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Blocked",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.appName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFEF2F2), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = log.threatType.uppercase(),
                            fontSize = 8.sp,
                            color = Color(0xFFB91C1C),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = log.packageName,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = dateText,
                    fontSize = 9.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

// ------------------------------------------------------------------------
// TAB 3: STORAGE JUNK CLEANER
// ------------------------------------------------------------------------

@Composable
fun StorageCleanerScreen(viewModel: SecurityViewModel) {
    val junkState by viewModel.junkState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Junk Meter Card
        FrostedGlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "정크 및 시스템 캐시 최적화 도구",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD97706),
                    letterSpacing = 1.5.sp
                )
                
                Spacer(modifier = Modifier.height(14.dp))

                when (val state = junkState) {
                    is JunkCleanState.Idle -> {
                        Text(
                            text = "0.0 GB",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                        Text(
                            text = "시스템 최적화 완료 및 안전한 정화 상태입니다",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { viewModel.startJunkScan() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("정크 디렉토리 재탐색", fontWeight = FontWeight.Bold)
                        }
                    }
                    is JunkCleanState.Scanning -> {
                        CircularProgressIndicator(
                            color = Color(0xFFF59E0B),
                            modifier = Modifier.size(50.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "불필요한 잔여 정크 탐색 중...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = state.currentFolder,
                            fontSize = 10.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    is JunkCleanState.ReadyToClean -> {
                        Text(
                            text = "${state.junkSizeGb} GB",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF59E0B)
                        )
                        Text(
                            text = "안전하게 비울 캐시 레지스터 및 정크가 준비되었습니다",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.cleanJunk() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("action_clean_junk")
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "청소")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("지금 정크 파일 완전히 비우기", fontWeight = FontWeight.Bold)
                        }
                    }
                    is JunkCleanState.Cleaning -> {
                        Text(
                            text = "캐시 로그 및 정크 파일 영구 안전 삭제 중...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { state.progress },
                            color = Color(0xFFF59E0B),
                            trackColor = Color(0xFFFEF3C7),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                    is JunkCleanState.Cleaned -> {
                        Text(
                            text = "✨ ${state.freedGb} GB 확보 완료",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                        Text(
                            text = "1.24 GB의 불필요한 시스템 임시 쓰레기 로그가 정상 비워졌습니다. 저장 공간 활성화!",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // Subdirectories logs listing
        Text(
            text = "정밀 검출 상세 내역",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0x66FFFFFF)),
            border = BorderStroke(1.dp, Color(0x54FFFFFF)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                JunkDetailsRow(title = "애플리케이션 로그 캐시", size = "412 MB")
                HorizontalDivider(color = Color(0x1F000000), modifier = Modifier.padding(vertical = 10.dp))
                JunkDetailsRow(title = "사용하지 않는 이전 APK 다운로드 파일", size = "280 MB")
                HorizontalDivider(color = Color(0x1F000000), modifier = Modifier.padding(vertical = 10.dp))
                JunkDetailsRow(title = "시스템 임시 분석 로그 파일", size = "512 MB")
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun JunkDetailsRow(title: String, size: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = title,
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, fontSize = 13.sp, color = Color(0xFF334155))
        }
        Text(text = size, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
    }
}

// ------------------------------------------------------------------------
// TAB 4: THREAT LOGS (SCAN HISTORY REPORTING)
// ------------------------------------------------------------------------

@Composable
fun SecurityLogsScreen(viewModel: SecurityViewModel) {
    val reports by viewModel.scanReports.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Maintenance Options
        FrostedGlassPanel(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "🎛️ 보안 관리 및 시스템 시뮬레이션 진단",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4F46E5),
                letterSpacing = 1.25.sp
            )
            Text(
                text = "테스트용 모의 전염 샌드박스를 원래대로 원격 배치하거나, 차단 기록 및 누적 검사기 데이터베이스를 최적 지워 검사 속도와 실시간 성능을 평가합니다.",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.resetAppMalware() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("action_reset_malware")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "재설정")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "모의 위협 배치", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { viewModel.clearAllHistory() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("action_clear_all_logs")
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "기록 삭제")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "전체 기록 비우기", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // System Reports Listing
        Text(
            text = "보안 검사 및 위협 보호 기록 (${reports.size}개)",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        if (reports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "기록 없음",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "보호 이력 데이터베이스가 깨끗하게 비어 있습니다.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                reports.forEach { report ->
                    ReportItemRow(report)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun ReportItemRow(report: ScanReport) {
    val dateText = remember(report.timestamp) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sdf.format(Date(report.timestamp))
    }

    val badgeColor = when (report.status) {
        "SAFE" -> Color(0xFF10B981)
        "THREAT_FOUND" -> Color(0xFFEF4444)
        else -> Color(0xFF3B82F6) // CLEANED / SANITIZED
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x66FFFFFF)),
        border = BorderStroke(1.dp, Color(0x54FFFFFF)),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = report.scanType,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = report.status,
                        fontSize = 9.sp,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (report.itemsScanned > 0) {
                Text(
                    text = "정밀 검사 완료된 항목 수: ${report.itemsScanned}개",
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )
            }
            
            if (report.detectedMalwareList.isNotEmpty()) {
                Text(
                    text = "발견 및 격리 위협 내용: ${report.detectedMalwareList}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (report.status == "THREAT_FOUND") Color(0xFFB91C1C) else Color(0xFF1E3A8A)
                )
            }

            Text(
                text = dateText,
                fontSize = 10.sp,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
