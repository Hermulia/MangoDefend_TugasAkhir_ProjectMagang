package com.riset.mangodefendd.ui.screens

import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.riset.mangodefendd.navigation.Routes
import com.riset.mangodefendd.ui.components.ThreatConfirmationDialog
import com.riset.mangodefendd.ui.viewmodel.AuthViewModel
import com.riset.mangodefendd.ui.viewmodel.DashboardViewModel
import com.riset.mangodefendd.util.PermissionUtils
import kotlinx.coroutines.delay
import java.util.Locale

// Reusing colors defined for History but ensuring they match Dashboard
val DashDarkBg = Color(0xFF0B0E14)
val DashCardBg = Color(0xFF151A23)
val DashGreen = Color(0xFF00FF41)
val DashGrey = Color(0xFF8A8D91)
val DashRed = Color(0xFFC62828)
val DashYellow = Color(0xFFFBC02D)
val DashBlue = Color(0xFF0277BD)

@Composable
fun DashboardScreen(
    navController: NavController,
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onNavigateToScan: () -> Unit,
    onNavigateToScanFolder: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSubscriptions: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val dashboardState by dashboardViewModel.dashboardState.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current

    var showLoginPrompt by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Notifications are required for scan alerts", Toast.LENGTH_SHORT).show()
        }
    }

    val checkNotificationPermission: () -> Boolean = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!isGranted) {
                notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            isGranted
        } else {
            true
        }
    }

    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { token ->
                    authViewModel.signInWithGoogle(token)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    val requireLogin: (() -> Unit) -> Unit = { action ->
        if (authState.isLoggedIn) {
            action()
        } else {
            showLoginPrompt = true
        }
    }

    Scaffold(
        containerColor = DashDarkBg,
        bottomBar = {
            NavigationBar(
                containerColor = DashDarkBg,
                contentColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Filled.Security, contentDescription = "Protect") },
                    label = { Text("Protect") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DashGreen,
                        selectedTextColor = DashGreen,
                        unselectedIconColor = DashGrey,
                        unselectedTextColor = DashGrey,
                        indicatorColor = DashGreen.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onNavigateToHistory() },
                    icon = { Icon(Icons.Filled.History, contentDescription = "History") },
                    label = { Text("History") },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = DashGrey,
                        unselectedTextColor = DashGrey,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onNavigateToSubscriptions() },
                    icon = { Icon(Icons.Filled.Payments, contentDescription = "Pricing") },
                    label = { Text("Pricing") },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = DashGrey,
                        unselectedTextColor = DashGrey,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onNavigateToProfile() },
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = DashGrey,
                        unselectedTextColor = DashGrey,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Security,
                        contentDescription = null,
                        tint = DashGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "MangoDefend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DashGreen
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = DashGrey)
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DashCardBg)
                            .clickable { onNavigateToProfile() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (authState.isLoggedIn && authState.user?.photoUrl != null) {
                            AsyncImage(
                                model = authState.user?.photoUrl,
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Filled.AccountCircle,
                                contentDescription = "Profile",
                                tint = DashGrey,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Welcome
            Text(
                text = if (authState.isLoggedIn) "Welcome back," else "Welcome to MangoDefend",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "System status: ",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
                Text(
                    text = "Secure",
                    style = MaterialTheme.typography.bodyLarge,
                    color = DashGreen,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Stats Grid (2x2)
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard(
                        label = "Total Scanned",
                        value = if (dashboardState.totalScanned >= 1000000) String.format(Locale.getDefault(), "%.1fM+", dashboardState.totalScanned / 1000000.0) else dashboardState.totalScanned.toString(),
                        icon = Icons.Filled.Autorenew,
                        accentColor = DashGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Malware Detected",
                        value = dashboardState.malwareDetected.toString(),
                        icon = Icons.Filled.BugReport,
                        accentColor = DashRed,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard(
                        label = "Suspicious Files",
                        value = dashboardState.suspiciousFiles.toString(),
                        icon = Icons.Filled.Warning,
                        accentColor = DashYellow,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Safe Files",
                        value = if (dashboardState.totalScanned > 0) String.format(Locale.getDefault(), "%.1f%%", (dashboardState.safeFiles.toFloat() / dashboardState.totalScanned * 100)) else "0%",
                        icon = Icons.Filled.Shield,
                        accentColor = DashBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons (2 items)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ActionCard(
                    label = "Scan File",
                    icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                    onClick = { requireLogin(onNavigateToScan) },
                    modifier = Modifier.weight(1f)
                )
                ActionCard(
                    label = "Scan Folder",
                    icon = Icons.Filled.Folder,
                    onClick = { requireLogin(onNavigateToScanFolder) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Circular Scan Button
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularScanButton(
                    isScanning = dashboardState.isScanning,
                    progress = dashboardState.scanProgress,
                    max = dashboardState.scanProgressMax,
                    onClick = { 
                        if (dashboardState.isScanning) {
                            dashboardViewModel.stopTotalScan()
                        } else {
                            requireLogin { 
                                if (PermissionUtils.hasStoragePermission(context)) {
                                    if (checkNotificationPermission()) {
                                        dashboardViewModel.startTotalScan(onLimitReached = {
                                            Toast.makeText(context, "Scan limit reached for your plan. Please upgrade.", Toast.LENGTH_SHORT).show()
                                        })
                                    }
                                } else {
                                    navController.navigate(Routes.Permission)
                                }
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Last Scan Relative Time
            var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(key1 = dashboardState.lastScanTimestamp) {
                while (true) {
                    delay(60000)
                    currentTime = System.currentTimeMillis()
                }
            }

            val lastScanText = remember(dashboardState.lastScanTimestamp, currentTime) {
                val timestamp = dashboardState.lastScanTimestamp
                if (timestamp == null) {
                    "No scans performed yet"
                } else {
                    val diff = System.currentTimeMillis() - timestamp
                    when {
                        diff < 60000 -> "Last scan performed just now"
                        diff < 3600000 -> "Last scan performed ${diff / 60000}m ago"
                        diff < 86400000 -> "Last scan performed ${diff / 3600000}h ago"
                        else -> "Last scan performed > 1 day ago"
                    }
                }
            }
            Text(
                text = lastScanText,
                style = MaterialTheme.typography.bodyMedium,
                color = DashGrey,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Realtime Protection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DashCardBg),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(DashDarkBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.WifiTethering, contentDescription = null, tint = DashBlue)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Real-time Protection", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Active & Monitoring", style = MaterialTheme.typography.bodySmall, color = DashGrey)
                    }
                    Switch(
                        checked = dashboardState.isRealtimeActive,
                        onCheckedChange = { enabled ->
                            requireLogin { 
                                if (enabled) {
                                    if (PermissionUtils.hasStoragePermission(context)) {
                                        if (checkNotificationPermission()) {
                                            dashboardViewModel.toggleRealtimeProtection(true)
                                        }
                                    } else {
                                        navController.navigate(Routes.Permission)
                                    }
                                } else {
                                    dashboardViewModel.toggleRealtimeProtection(false)
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = DashGreen,
                            uncheckedThumbColor = DashGrey,
                            uncheckedTrackColor = DashCardBg,
                            uncheckedBorderColor = DashGrey
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showLoginPrompt) {
        AlertDialog(
            onDismissRequest = { showLoginPrompt = false },
            title = { Text("Feature Restricted") },
            text = { Text("Please login with your Google account to access this security feature.") },
            confirmButton = {
                TextButton(onClick = {
                    showLoginPrompt = false
                    onNavigateToProfile()
                }) {
                    Text("Login Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLoginPrompt = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Threat Confirmation Dialog
    dashboardState.pendingThreat?.let { event ->
        ThreatConfirmationDialog(
            event = event,
            onResolve = { action ->
                dashboardViewModel.resolvePendingThreat(action)
            }
        )
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    showLine: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(130.dp),
        colors = CardDefaults.cardColors(containerColor = DashCardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    if (showLine) {
                        val strokeWidth = 4.dp.toPx()
                        val y = size.height - strokeWidth / 2
                        drawLine(
                            color = accentColor,
                            start = Offset(0f, y),
                            end = Offset(size.width * 0.6f, y),
                            strokeWidth = strokeWidth
                        )
                    }
                }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = DashGrey,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun ActionCard(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(110.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DashCardBg),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DashGreen,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun CircularScanButton(
    isScanning: Boolean,
    progress: Int,
    max: Int,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(240.dp)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(DashGreen.copy(alpha = 0.3f), Color.Transparent)
                    ),
                    radius = size.width / 2
                )
            }
    ) {
        // Outer Glow Effect
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape)
                .border(2.dp, DashGreen.copy(alpha = 0.5f), CircleShape)
                .padding(8.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onClick() },
                shape = CircleShape,
                color = if (isScanning) Color.DarkGray else DashGreen,
                shadowElevation = 8.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            color = DashGreen,
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (max > 0) "$progress / $max" else "Scanning...",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            Icons.Filled.HealthAndSafety, 
                            contentDescription = null, 
                            tint = Color.Black, 
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "START FULL SCAN",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
        
        if (isScanning && max > 0) {
            Canvas(modifier = Modifier.size(200.dp)) {
                drawArc(
                    color = Color.White,
                    startAngle = -90f,
                    sweepAngle = (progress.toFloat() / max * 360f),
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx())
                )
            }
        }
    }
}
