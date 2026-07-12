package com.riset.mangodefendd.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.riset.mangodefendd.data.scan.ScanResultEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryDetailDialog(
    result: ScanResultEntity,
    onDeleteFile: (ScanResultEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val isDangerous = result.status.uppercase() == "DANGEROUS"
    val isSuspicious = result.status.uppercase() == "SUSPICIOUS"
    val isDeleted = result.status.uppercase() == "DELETED" || result.status.uppercase() == "TERHAPUS"
    val canDelete = (isDangerous || isSuspicious) && !isDeleted

    val displayPath = if (result.filePath.startsWith("content://")) {
        "System Gallery / Protected Storage"
    } else if (result.filePath.isEmpty()) {
        "Remote Scan / Unknown"
    } else {
        result.filePath
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                when {
                    result.status.uppercase() == "SAFE" -> Icons.Default.CheckCircle
                    isDeleted -> Icons.Default.DeleteForever
                    else -> Icons.Default.Warning
                },
                contentDescription = null,
                tint = when {
                    result.status.uppercase() == "SAFE" -> Color(0xFF00FF41)
                    result.status.uppercase() == "DANGEROUS" -> Color(0xFFC62828)
                    isDeleted -> Color.Gray
                    else -> Color(0xFFFBC02D)
                },
                modifier = Modifier.size(40.dp)
            )
        },
        title = {
            Text(
                text = "Scan Details",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                DetailItem("File Name", result.fileName)
                DetailItem("Source Location", displayPath)
                DetailItem("Status", result.status, 
                    color = when {
                        result.status.uppercase() == "SAFE" -> Color(0xFF00FF41)
                        result.status.uppercase() == "DANGEROUS" -> Color(0xFFC62828)
                        isDeleted -> Color.Gray
                        else -> Color(0xFFFBC02D)
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("BENIGN", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(String.format(Locale.getDefault(), "%.1f%%", result.benignScore * 100), fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("MALWARE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(String.format(Locale.getDefault(), "%.1f%%", result.malwareScore * 100), fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                DetailItem("Scan Date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(result.scanDate)))
                
                if (canDelete) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "This file is identified as a threat. You can delete it from your storage now.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        },
        confirmButton = {
            if (canDelete) {
                Button(
                    onClick = { onDeleteFile(result) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("DELETE FILE", color = Color.White)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("CLOSE")
                }
            }
        },
        dismissButton = {
            if (canDelete) {
                TextButton(onClick = onDismiss) {
                    Text("CANCEL")
                }
            }
        }
    )
}

@Composable
private fun DetailItem(label: String, value: String, color: Color = Color.Unspecified) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = if (color == Color.Unspecified) Color.White else color, fontSize = 14.sp)
    }
}
