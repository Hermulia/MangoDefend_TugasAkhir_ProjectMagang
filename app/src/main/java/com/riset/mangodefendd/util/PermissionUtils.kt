package com.riset.mangodefendd.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment

object PermissionUtils {
    /**
     * Check if MANAGE_EXTERNAL_STORAGE permission is granted (API 30+)
     * For devices before API 30, uses READ_EXTERNAL_STORAGE
     */
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Open Android Settings to grant MANAGE_EXTERNAL_STORAGE (API 30+)
     */
    fun openStorageSettingsIntent(context: Context): Intent {
        return Intent(
            android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
        }
    }

    /**
     * Check if device can use scoped storage (API 30+)
     */
    fun supportsScopedStorage(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    }
}




