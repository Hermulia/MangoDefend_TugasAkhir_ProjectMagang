package com.riset.mangodefendd.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.riset.mangodefendd.ml.ThreatEvent
import com.riset.mangodefendd.ml.ScanAction

@Composable
fun ThreatConfirmationDialog(
    event: ThreatEvent,
    onResolve: (ScanAction) -> Unit
) {
    val entity = event.entity
    val isMalware = entity.status.uppercase() == "DANGEROUS"
    
    AlertDialog(
        onDismissRequest = { /* No dismiss, must choose */ },
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = if (isMalware) Color.Red else Color.Yellow) },
        title = {
            Text(
                text = if (isMalware) "Malware Detected!" else "Suspicious File Found",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text("File: ${entity.fileName}", fontWeight = FontWeight.SemiBold)
                Text("Path: ${entity.filePath}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (isMalware) 
                        "This file is highly likely to be malicious. We recommend deleting it immediately to protect your device."
                        else "This file shows suspicious patterns. Do you want to keep or delete it?",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onResolve(ScanAction.DELETE) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("DELETE FILE", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onResolve(ScanAction.KEEP) }
            ) {
                Text("KEEP FILE")
            }
        }
    )
}
