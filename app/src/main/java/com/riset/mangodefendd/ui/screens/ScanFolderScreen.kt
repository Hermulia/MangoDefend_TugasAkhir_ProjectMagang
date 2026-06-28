package com.riset.mangodefendd.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.riset.mangodefendd.data.scan.ScanResultEntity
import com.riset.mangodefendd.ui.components.ThreatConfirmationDialog
import com.riset.mangodefendd.ui.theme.CardBackground
import com.riset.mangodefendd.ui.theme.DarkBackground
import com.riset.mangodefendd.ui.theme.MalwareRed
import com.riset.mangodefendd.ui.theme.NeonGreen
import com.riset.mangodefendd.ui.viewmodel.AuthViewModel
import com.riset.mangodefendd.ui.viewmodel.DashboardViewModel
import com.riset.mangodefendd.ui.viewmodel.ScanViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ScanFolderScreen(
    viewModel: ScanViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authState by authViewModel.authState.collectAsState()
    val dashboardState by dashboardViewModel.dashboardState.collectAsState()
    val pendingThreat by viewModel.pendingThreat.collectAsState()
    
    var lastBatchResult by remember { mutableStateOf<List<ScanResultEntity>?>(null) }
    var isScanningInternal by remember { mutableStateOf(false) }

    val engineVersion = dashboardState.activeSubscription?.plan?.model?.version ?: "v4.8.2-Core"

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

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            isScanningInternal = true
            scope.launch(Dispatchers.IO) {
                val filesToScan = mutableListOf<File>()
                val deleteCallbacks = mutableMapOf<String, suspend () -> Unit>()
                
                val documentFile = DocumentFile.fromTreeUri(context, uri)
                if (documentFile != null && documentFile.isDirectory) {
                    collectFilesRecursiveWithDeletion(context, documentFile, filesToScan, deleteCallbacks)
                }
                
                withContext(Dispatchers.Main) {
                    if (filesToScan.isNotEmpty()) {
                        viewModel.scanBatch(filesToScan, deleteCallbacks, { _, _ -> }) { results ->
                            lastBatchResult = results
                            isScanningInternal = false
                            filesToScan.forEach { if (it.exists()) it.delete() }
                        }
                    } else {
                        isScanningInternal = false
                    }
                }
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
                Icon(Icons.Filled.Security, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("MangoDefend", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = NeonGreen)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { /* Settings */ }) { Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.Gray) }
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.DarkGray)) {
                    if (authState.isLoggedIn) {
                        AsyncImage(model = authState.user?.photoUrl, contentDescription = "Profile", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Profile", tint = Color.Gray, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Combined Status Card
        Card(modifier = Modifier.fillMaxWidth().height(100.dp), colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(24.dp)) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("CURRENT STATUS", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(if (isScanningInternal) Color.Yellow else NeonGreen, CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isScanningInternal) "Scanning..." else "Idle", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Engine Version", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(engineVersion, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Large Selection Area
        Box(modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(32.dp)).clickable { if (!isScanningInternal) launcher.launch(null) }, contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f))
                drawRoundRect(color = Color.Gray.copy(alpha = 0.3f), style = stroke, cornerRadius = androidx.compose.ui.geometry.CornerRadius(32.dp.toPx()))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(32.dp)) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Surface(modifier = Modifier.size(120.dp), shape = RoundedCornerShape(24.dp), color = Color.White.copy(alpha = 0.05f)) {
                        Icon(imageVector = Icons.Filled.Folder, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(24.dp).fillMaxSize())
                    }
                    Surface(modifier = Modifier.size(36.dp).offset(x = 6.dp, y = 6.dp), shape = CircleShape, color = CardBackground, border = BorderStroke(2.dp, DarkBackground)) {
                        Icon(imageVector = Icons.Filled.Settings, contentDescription = null, tint = NeonGreen, modifier = Modifier.padding(8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Text("Select Folder", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Tap to choose a directory for\nrecursive heuristic analysis.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, textAlign = TextAlign.Center, lineHeight = 22.sp)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Bottom Grid
        Row(modifier = Modifier.fillMaxWidth().height(180.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(modifier = Modifier.weight(1.3f).fillMaxHeight(), colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(24.dp)) {
                val hasScanned = lastBatchResult != null
                val totalCount = lastBatchResult?.size ?: 0
                val malwareCount = lastBatchResult?.count { it.status != "Safe" } ?: 0
                val cleanCount = totalCount - malwareCount
                val statusText = if (!hasScanned) "-" else if (malwareCount > 0) "Danger" else "Secure"
                val progress = if (totalCount > 0) cleanCount.toFloat() / totalCount else if (hasScanned) 1f else 0f
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ANALYSIS SUMMARY", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(if (hasScanned) "$totalCount Files\nScanned" else "- Files\nScanned", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Column {
                            Text("CLEAN", style = MaterialTheme.typography.labelSmall, color = NeonGreen, fontSize = 8.sp)
                            Text(if (hasScanned) "$cleanCount" else "-", color = NeonGreen, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("THREATS", style = MaterialTheme.typography.labelSmall, color = MalwareRed, fontSize = 8.sp)
                            Text(if (hasScanned) "$malwareCount" else "-", color = MalwareRed, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("STATUS", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 8.sp)
                            Text(statusText, color = if (malwareCount > 0) MalwareRed else Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Color.DarkGray.copy(alpha = 0.5f))) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress).background(if (malwareCount > 0) MalwareRed else NeonGreen, CircleShape))
                    }
                }
            }
            Card(modifier = Modifier.weight(0.7f).fillMaxHeight(), colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("LAST UPDATE", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AccessTime, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(lastScanText, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    pendingThreat?.let { event ->
        ThreatConfirmationDialog(event = event, onResolve = { action -> viewModel.resolvePendingThreat(action) })
    }
}

private fun collectFilesRecursiveWithDeletion(
    context: Context, 
    directory: DocumentFile, 
    files: MutableList<File>,
    deleteCallbacks: MutableMap<String, suspend () -> Unit>
) {
    directory.listFiles().forEach { file ->
        if (file.isDirectory) {
            collectFilesRecursiveWithDeletion(context, file, files, deleteCallbacks)
        } else if (file.isFile) {
            val tmp = File(context.cacheDir, "scan_${System.currentTimeMillis()}_${file.name}")
            try {
                context.contentResolver.openInputStream(file.uri)?.use { input ->
                    tmp.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (tmp.exists()) {
                    files.add(tmp)
                    deleteCallbacks[tmp.absolutePath] = {
                        try {
                            file.delete()
                        } catch (e: Exception) {}
                    }
                }
            } catch (e: Exception) {}
        }
    }
}
