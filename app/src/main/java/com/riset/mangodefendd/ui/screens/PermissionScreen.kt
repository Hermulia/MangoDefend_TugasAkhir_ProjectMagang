package com.riset.mangodefendd.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.riset.mangodefendd.util.PermissionUtils

@Composable
fun PermissionRequestScreen(onPermissionGranted: () -> Unit) {
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(PermissionUtils.hasStoragePermission(context))
    }

    if (permissionGranted) {
        onPermissionGranted()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Storage Permission Required",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (PermissionUtils.supportsScopedStorage()) {
            Text(
                "MangoDefendd needs permission to scan files on your device.\n\n" +
                        "You'll be redirected to Settings to grant access.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Button(
                onClick = {
                    val intent = PermissionUtils.openStorageSettingsIntent(context)
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Open Settings")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Steps:\n" +
                        "1. Tap 'Open Settings'\n" +
                        "2. Toggle 'Allow access to manage all files'\n" +
                        "3. Return to app",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            Text(
                "MangoDefendd needs permission to scan files on your device.\n\n" +
                        "Please grant file access in Settings > App Permissions > Files.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Button(
                onClick = {
                    permissionGranted = PermissionUtils.hasStoragePermission(context)
                    if (!permissionGranted) {
                        val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                            addCategory(Intent.CATEGORY_DEFAULT)
                        }
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Open Settings")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Without this permission, scanning will be limited to your app's private storage.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun SafGuideScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            "Scoped Storage (Android 11+)",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Your device uses Scoped Storage. You can still scan files from:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                BulletPoint("• Downloads folder")
                BulletPoint("• Documents folder (from file picker)")
                BulletPoint("• Files app selections")
                BulletPoint("• Installed APKs via PackageManager")

                Text(
                    "\nTo scan a specific file:\n" +
                            "1. Tap \"Scan File\"\n" +
                            "2. Select from the file picker\n" +
                            "3. App will analyze it",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

@Composable
fun BulletPoint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

