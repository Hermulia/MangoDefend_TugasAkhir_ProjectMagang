package com.riset.mangodefendd.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.riset.mangodefendd.ui.viewmodel.ScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    scanViewModel: ScanViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    val scanState by scanViewModel.scanState.collectAsState()
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var isEditMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { 
                Text(if (isEditMode) "${selectedIds.size} Selected" else "Scan History") 
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
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                            scanViewModel.deleteHistoryItems(selectedIds.toList())
                            isEditMode = false
                            selectedIds = emptySet()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = Color.Red)
                        }
                    } else {
                        IconButton(onClick = { isEditMode = true }) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Edit Mode")
                        }
                        IconButton(onClick = { scanViewModel.clearHistory() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear History", tint = Color.Red)
                        }
                    }
                }
            }
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (scanState.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                ) {
                    Text("No scans yet", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn {
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
                                    }
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
                                        }
                                    }
                            )
                        }
                    }
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
        modifier = modifier
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(fileName, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(4.dp))

            val statusColor = when (status.uppercase()) {
                "SAFE" -> MaterialTheme.colorScheme.primary
                "SUSPICIOUS" -> MaterialTheme.colorScheme.tertiary
                "DANGEROUS" -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            }
            Text(
                "Status: $status",
                style = MaterialTheme.typography.bodySmall,
                color = statusColor
            )

            Row(modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    "Benign: ${String.format("%.1f", benignScore * 100)}%",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Malware: ${String.format("%.1f", malwareScore * 100)}%",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                "Scanned: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(scanDate)}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

