package com.riset.mangodefendd.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import androidx.compose.ui.Alignment
import com.riset.mangodefendd.ui.viewmodel.ScanViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.Text
import com.riset.mangodefendd.util.FileUtils
import android.net.Uri
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: ScanViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    var status by remember { mutableStateOf("Idle") }
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            // copy to cache and scan
            val tmp = File(context.cacheDir, "picked_${System.currentTimeMillis()}")
            try {
                FileUtils.copyUriToFile(context, uri, tmp)
                status = "Scanning..."
                viewModel.scanSingleFile(tmp) { res ->
                    status = res?.let { "${it.status} (${String.format("%.2f", it.malwareScore*100)}%)" } ?: "Scan failed"
                }
            } catch (e: Exception) {
                status = "Error: ${e.message}"
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar with back button
        TopAppBar(
            title = { Text("Scan File") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
        
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)) {
            Button(onClick = { launcher.launch("*/*") }) {
                Text("Choose File")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Status: $status")
        }
    }
}



