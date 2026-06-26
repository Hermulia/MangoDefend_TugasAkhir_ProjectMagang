package com.riset.mangodefendd.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Scan History") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                        ScanResultCard(
                            fileName = result.fileName,
                            status = result.status,
                            malwareScore = result.malwareScore,
                            benignScore = result.benignScore,
                            scanDate = result.scanDate
                        )
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
    scanDate: Long
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(fileName, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(4.dp))

            val statusColor = when (status) {
                "Safe" -> MaterialTheme.colorScheme.primary
                "Suspicious" -> MaterialTheme.colorScheme.tertiary
                "Dangerous" -> MaterialTheme.colorScheme.error
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

