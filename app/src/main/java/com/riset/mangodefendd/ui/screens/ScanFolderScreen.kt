package com.riset.mangodefendd.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.riset.mangodefendd.ui.viewmodel.ScanViewModel
import java.io.File
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanFolderScreen(
    viewModel: ScanViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
) {
    var status by remember { mutableStateOf("Idle") }
    var progress by remember { mutableFloatStateOf(0f) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            status = "Reading folder..."
            scope.launch {
                val filesToScan = withContext(Dispatchers.IO) {
                    val root = DocumentFile.fromTreeUri(context, uri)
                    val list = mutableListOf<File>()
                    root?.listFiles()?.forEach { doc ->
                        if (doc.isFile) {
                            // Copy to temp file to scan since ONNX needs a file path
                            val tmpFile = File(context.cacheDir, "scan_${doc.name}")
                            context.contentResolver.openInputStream(doc.uri)?.use { input ->
                                tmpFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            list.add(tmpFile)
                        }
                    }
                    list
                }

                if (filesToScan.isEmpty()) {
                    status = "No files found in folder"
                } else {
                    status = "Scanning ${filesToScan.size} files..."
                    viewModel.scanBatch(
                        filesToScan,
                        progress = { scanned, total ->
                            progress = scanned.toFloat() / total.toFloat()
                        }
                    ) { results ->
                        val threats = results.count { it.status == "Dangerous" || it.status == "Suspicious" }
                        status = "Scan Complete: ${results.size} files checked. $threats threats found."
                        
                        // Clean up temp files
                        filesToScan.forEach { it.delete() }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Folder") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(onClick = { launcher.launch(null) }) {
                Text("Select Folder")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(status)
            
            if (progress in 0.001f..0.999f) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
