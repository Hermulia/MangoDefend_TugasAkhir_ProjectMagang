package com.riset.mangodefendd.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.riset.mangodefendd.data.scan.ScanResultEntity
import com.riset.mangodefendd.ui.theme.CardBackground
import com.riset.mangodefendd.ui.theme.DarkBackground
import com.riset.mangodefendd.ui.theme.MalwareRed
import com.riset.mangodefendd.ui.theme.NeonGreen
import com.riset.mangodefendd.ui.viewmodel.AuthViewModel
import com.riset.mangodefendd.ui.viewmodel.DashboardViewModel
import com.riset.mangodefendd.ui.viewmodel.ScanViewModel
import com.riset.mangodefendd.util.FileUtils
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun ScanScreen(
    viewModel: ScanViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val dashboardState by dashboardViewModel.dashboardState.collectAsState()
    var lastResult by remember { mutableStateOf<ScanResultEntity?>(null) }
    var isScanningInternal by remember { mutableStateOf(false) }

    // Engine version
    val engineVersion = dashboardState.activeSubscription?.plan?.model?.version ?: "v4.8.2-Core"

    // Last scan relative time logic
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000)
            currentTime = System.currentTimeMillis()
        }
    }

    val lastScanText = remember(dashboardState.lastScanTimestamp, currentTime) {
        val timestamp = dashboardState.lastScanTimestamp
        if (timestamp == null) {
            "Never"
        } else {
            val diff = currentTime - timestamp
            when {
                diff < 60000 -> "Just now"
                diff < 3600000 -> "${diff / 60000}m ago"
                diff < 86400000 -> "${diff / 3600000}h ago"
                else -> "${diff / 86400000}d ago"
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            isScanningInternal = true
            val tmp = File(context.cacheDir, "picked_${System.currentTimeMillis()}")
            try {
                FileUtils.copyUriToFile(context, uri, tmp)
                viewModel.scanSingleFile(tmp) { res ->
                    lastResult = res
                    isScanningInternal = false
                    tmp.delete()
                }
            } catch (e: Exception) {
                isScanningInternal = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonGreen)
                }
                Icon(
                    Icons.Filled.Security,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "MangoDefend",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { /* Settings */ }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.Gray)
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.DarkGray)
                ) {
                    if (authState.isLoggedIn) {
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
                            tint = Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth().height(100.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "CURRENT STATUS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(if (isScanningInternal) Color.Yellow else NeonGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isScanningInternal) "Scanning..." else "Idle",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Engine Version",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        engineVersion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Selection Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(32.dp))
                .clickable { if (!isScanningInternal) launcher.launch("*/*") },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
                )
                drawRoundRect(
                    color = Color.Gray.copy(alpha = 0.3f),
                    style = stroke,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(32.dp.toPx())
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.05f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    "Select File",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Tap to choose a file for\ndeep heuristic analysis.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Bottom Row
        Row(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Analysis Summary Card
            Card(
                modifier = Modifier.weight(1.3f).fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(24.dp)
            ) {
                val hasScanned = lastResult != null
                val statusText = if (!hasScanned) "-" else if (lastResult?.status == "Safe") "Secure" else lastResult?.status ?: "N/A"
                val benignPercent = lastResult?.benignScore?.let { (it * 100).toInt() } ?: 0
                val malwarePercent = lastResult?.malwareScore?.let { (it * 100).toInt() } ?: 0

                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "ANALYSIS SUMMARY",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        if (hasScanned) "1 File\nScanned" else "- File\nScanned",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text("BENIGN", style = MaterialTheme.typography.labelSmall, color = NeonGreen, fontSize = 8.sp)
                            Text(if (hasScanned) "$benignPercent%" else "-", color = NeonGreen, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("MALWARE", style = MaterialTheme.typography.labelSmall, color = MalwareRed, fontSize = 8.sp)
                            Text(if (hasScanned) "$malwarePercent%" else "-", color = MalwareRed, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("STATUS", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 8.sp)
                            Text(statusText, color = if (statusText == "Secure") NeonGreen else if (statusText == "-") Color.White else MalwareRed, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Color.DarkGray.copy(alpha = 0.5f))
                    ) {
                        val progress = if (hasScanned) (benignPercent / 100f) else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .background(if (statusText == "Secure") NeonGreen else MalwareRed, CircleShape)
                        )
                    }
                }
            }

            // Last Update Card
            Card(
                modifier = Modifier.weight(0.7f).fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "LAST UPDATE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.AccessTime,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            lastScanText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
