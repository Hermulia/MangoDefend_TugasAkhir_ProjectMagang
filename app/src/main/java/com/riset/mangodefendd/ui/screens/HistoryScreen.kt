package com.riset.mangodefendd.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.riset.mangodefendd.data.scan.ScanResultEntity
import com.riset.mangodefendd.ui.components.HistoryDetailDialog
import com.riset.mangodefendd.ui.viewmodel.ScanViewModel
import java.text.SimpleDateFormat
import java.util.*

// Colors based on the provided design image
val DarkBg = Color(0xFF0B0E14)
val CardBg = Color(0xFF151A23)
val BrandGreen = Color(0xFF00FF41)
val BrandGrey = Color(0xFF8A8D91)
val BrandRed = Color(0xFFC62828)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    scanViewModel: ScanViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    val scanState by scanViewModel.scanState.collectAsState()
    val totalItems by scanViewModel.totalItems.collectAsState()
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var isEditMode by remember { mutableStateOf(false) }
    var selectedResultForDetail by remember { mutableStateOf<ScanResultEntity?>(null) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    var showDeleteSelectedConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBg,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                title = {
                    Text(
                        if (isEditMode) "${selectedIds.size} Selected" else "Scan History",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isEditMode) {
                            isEditMode = false
                            selectedIds = emptySet()
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (scanState.isNotEmpty()) {
                        if (isEditMode) {
                            IconButton(onClick = {
                                if (selectedIds.size == scanState.size) {
                                    selectedIds = emptySet()
                                } else {
                                    selectedIds = scanState.map { it.id }.toSet()
                                }
                            }) {
                                Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                            }
                            IconButton(onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    showDeleteSelectedConfirmation = true
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = BrandRed)
                            }
                        } else {
                            IconButton(onClick = { isEditMode = true }) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Edit Mode")
                            }
                            IconButton(onClick = { showClearConfirmation = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Clear History", tint = BrandRed)
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            if (scanState.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                ) {
                    Text("No scans yet", color = BrandGrey, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        SummaryCard(totalItems, scanState)
                    }

                    items(scanState) { result ->
                        val isSelected = selectedIds.contains(result.id)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isEditMode) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedIds = if (checked) {
                                            selectedIds + result.id
                                        } else {
                                            selectedIds - result.id
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = BrandGreen,
                                        uncheckedColor = BrandGrey,
                                        checkmarkColor = DarkBg
                                    )
                                )
                            }
                            ScanResultCard(
                                fileName = result.fileName,
                                status = result.status,
                                malwareScore = result.malwareScore,
                                benignScore = result.benignScore,
                                scanDate = result.scanDate,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        if (isEditMode) {
                                            selectedIds = if (isSelected) {
                                                selectedIds - result.id
                                            } else {
                                                selectedIds + result.id
                                            }
                                        } else if (result.status.uppercase() != "SAFE") {
                                            selectedResultForDetail = result
                                        }
                                    }
                            )
                        }
                    }

                    if (scanState.size < totalItems) {
                        item {
                            Button(
                                onClick = { scanViewModel.loadMore() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CardBg),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("LOAD MORE HISTORY", color = BrandGrey, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    selectedResultForDetail?.let { result ->
        HistoryDetailDialog(
            result = result,
            onDeleteFile = {
                scanViewModel.deleteFileAndHistory(it)
                selectedResultForDetail = null
            },
            onDismiss = { selectedResultForDetail = null }
        )
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear History", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently clear all scan history? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        scanViewModel.clearHistory()
                        showClearConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                ) {
                    Text("CLEAR ALL", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    if (showDeleteSelectedConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedConfirmation = false },
            title = { Text("Delete Selected", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete ${selectedIds.size} selected items from history?") },
            confirmButton = {
                Button(
                    onClick = {
                        scanViewModel.deleteHistoryItems(selectedIds.toList())
                        isEditMode = false
                        selectedIds = emptySet()
                        showDeleteSelectedConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                ) {
                    Text("DELETE", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedConfirmation = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
fun SummaryCard(totalScans: Int, scanResults: List<com.riset.mangodefendd.data.scan.ScanResultEntity>) {
    val lastScan = scanResults.maxByOrNull { it.scanDate }?.scanDate
    val lastScanStr = if (lastScan != null) {
        val today = Calendar.getInstance()
        val scanCal = Calendar.getInstance().apply { timeInMillis = lastScan }
        if (today.get(Calendar.DAY_OF_YEAR) == scanCal.get(Calendar.DAY_OF_YEAR)) {
            "Today, " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastScan))
        } else {
            SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(lastScan))
        }
    } else {
        "N/A"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Vertical green line indicator on the left
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .padding(vertical = 20.dp)
                    .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                    .background(BrandGreen)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text("ALL SYSTEMS", color = BrandGrey, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Text("Status: Protected", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row {
                        Column {
                            Text("Total Scans", color = BrandGrey, fontSize = 11.sp)
                            Text(String.format(Locale.getDefault(), "%,d", totalScans), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(48.dp))
                        Column {
                            Text("Last Scan", color = BrandGrey, fontSize = 11.sp)
                            Text(lastScanStr, color = BrandGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(BrandGreen.copy(alpha = 0.1f))
                        .border(1.dp, BrandGreen.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Shield, 
                        contentDescription = null, 
                        tint = BrandGreen, 
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ScanResultCard(
    fileName: String,
    status: String,
    malwareScore: Double,
    benignScore: Double,
    scanDate: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    fileName, 
                    style = MaterialTheme.typography.titleMedium, 
                    color = Color.White, 
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                
                val icon = when {
                    fileName.endsWith(".apk", true) -> Icons.Default.Android
                    fileName.endsWith(".jpg", true) || fileName.endsWith(".png", true) || fileName.endsWith(".jpeg", true) -> Icons.Default.Image
                    fileName.endsWith(".pdf", true) || fileName.endsWith(".doc", true) || fileName.endsWith(".docx", true) -> Icons.Default.Description
                    else -> Icons.AutoMirrored.Filled.InsertDriveFile
                }
                Icon(icon, contentDescription = null, tint = BrandGrey, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))

            val isSafe = status.uppercase() == "SAFE"
            val isDeleted = status.uppercase() == "TERHAPUS" || status.uppercase() == "DELETED"
            val statusColor = when {
                isSafe -> BrandGreen
                isDeleted -> Color.Gray
                else -> BrandRed
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    isSafe -> {
                        Icon(
                            Icons.Default.CheckCircle, 
                            contentDescription = null, 
                            tint = BrandGreen, 
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    isDeleted -> {
                        Icon(
                            Icons.Default.DeleteForever, 
                            contentDescription = null, 
                            tint = Color.Gray, 
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    else -> {
                        Icon(
                            Icons.Default.Warning, 
                            contentDescription = null, 
                            tint = BrandRed, 
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Status: $status",
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("BENIGN", color = BrandGrey, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Text(
                        String.format(Locale.getDefault(), "%.1f%%", benignScore * 100),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("MALWARE", color = BrandGrey, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Text(
                        String.format(Locale.getDefault(), "%.1f%%", malwareScore * 100),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccessTime, 
                    contentDescription = null, 
                    tint = BrandGrey, 
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Scanned: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(scanDate))}",
                    color = BrandGrey,
                    fontSize = 12.sp
                )
            }
        }
    }
}
