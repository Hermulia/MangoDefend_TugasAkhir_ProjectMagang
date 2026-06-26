package com.riset.mangodefendd.ui.screens

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.riset.mangodefendd.ui.viewmodel.AuthViewModel
import com.riset.mangodefendd.ui.viewmodel.DashboardViewModel
import com.riset.mangodefendd.util.FileUtils
import java.io.File

@Composable
fun DashboardScreen(
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onNavigateToScan: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSubscriptions: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val dashboardState by dashboardViewModel.dashboardState.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current

    var showLoginPrompt by remember { mutableStateOf(false) }

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

    // Launcher for Scan File (Quick Action)
    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val tmp = File(context.cacheDir, "quick_scan_${System.currentTimeMillis()}")
            try {
                FileUtils.copyUriToFile(context, uri, tmp)
                onNavigateToScan() // Navigate to scan screen where the logic usually lives or handle here
            } catch (e: Exception) {
                // Handle error
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
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = Color.White
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onNavigateToHistory() },
                    icon = { Icon(Icons.Filled.History, contentDescription = "History") },
                    label = { Text("History") },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onNavigateToSubscriptions() },
                    icon = { Icon(Icons.Filled.Payments, contentDescription = "Pricing") },
                    label = { Text("Pricing") },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onNavigateToProfile() },
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
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
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "MangoDefend",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row {
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.Gray)
                    }
                    IconButton(onClick = { onNavigateToProfile() }) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Profile", tint = Color.Gray, modifier = Modifier.size(32.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Welcome
            Text(
                text = "Welcome back,",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Row {
                Text(
                    text = "System status: ",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
                Text(
                    text = "Secure",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Stats Grid
            Row(modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    label = "Total Scanned",
                    value = if (dashboardState.totalScanned >= 1000000) String.format("%.1fM+", dashboardState.totalScanned / 1000000.0) else dashboardState.totalScanned.toString(),
                    icon = Icons.Filled.Autorenew,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Malware Detected",
                    value = dashboardState.malwareDetected.toString(),
                    icon = Icons.Filled.BugReport,
                    accentColor = Color(0xFFFF4444),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    label = "Suspicious Files",
                    value = dashboardState.suspiciousFiles.toString(),
                    icon = Icons.Filled.Warning,
                    accentColor = Color(0xFFFFBB33),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Safe Files",
                    value = if (dashboardState.totalScanned > 0) String.format("%.1f%%", (dashboardState.safeFiles.toFloat() / dashboardState.totalScanned * 100)) else "0%",
                    icon = Icons.Filled.Shield,
                    accentColor = Color(0xFF33B5E5),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionCard(
                    label = "Scan File",
                    icon = Icons.Filled.InsertDriveFile,
                    onClick = { requireLogin { filePickerLauncher.launch("*/*") } },
                    modifier = Modifier.weight(1f)
                )
                ActionCard(
                    label = "Scan Folder",
                    icon = Icons.Filled.Folder,
                    onClick = { requireLogin(onNavigateToScan) },
                    modifier = Modifier.weight(1f)
                )
                ActionCard(
                    label = "Full System",
                    icon = Icons.Filled.Security,
                    onClick = { requireLogin { dashboardViewModel.startTotalScan() } },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Circular Scan Button
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularScanButton(
                    isScanning = dashboardState.isScanning,
                    onClick = { requireLogin { dashboardViewModel.startTotalScan() } }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(key1 = dashboardState.lastScanTimestamp) {
                while (true) {
                    kotlinx.coroutines.delay(60000) // Update every minute
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
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Realtime Protection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.background, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.WifiTethering, contentDescription = null, tint = Color(0xFF33B5E5))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Real-time Protection", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text("Active & Monitoring", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Switch(
                        checked = dashboardState.isRealtimeActive,
                        onCheckedChange = { enabled ->
                            requireLogin { dashboardViewModel.toggleRealtimeProtection(enabled) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Logout/Login Button
            if (!authState.isLoggedIn) {
                Button(
                    onClick = { googleLauncher.launch(authViewModel.getSignInIntent()) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Login with Google", color = Color.Black)
                }
            } else {
                 TextButton(
                    onClick = { authViewModel.signOut(); onLogout() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Logout", color = Color.Red)
                }
            }
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
                    googleLauncher.launch(authViewModel.getSignInIntent())
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
}

@Composable
fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(4.dp)
            .height(120.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val strokeWidth = 4.dp.toPx()
                    val y = size.height - strokeWidth / 2
                    drawLine(
                        color = accentColor,
                        start = Offset(0f, y),
                        end = Offset(size.width * 0.6f, y),
                        strokeWidth = strokeWidth
                    )
                }
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
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
            .padding(4.dp)
            .height(90.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CircularScanButton(
    isScanning: Boolean,
    onClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(220.dp)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.3f), Color.Transparent)
                    ),
                    radius = size.width / 2
                )
            }
    ) {
        Surface(
            modifier = Modifier
                .size(160.dp)
                .clickable { onClick() },
            shape = CircleShape,
            color = primaryColor,
            shadowElevation = 12.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(modifier = Modifier.size(4.dp).background(Color.Black, CircleShape))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isScanning) "SCANNING..." else "START FULL SCAN",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
