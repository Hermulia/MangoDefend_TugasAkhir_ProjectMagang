package com.riset.mangodefendd.ml

import android.content.Context
import android.content.pm.PackageManager
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ScanEngine {
    suspend fun gatherFilesUnderRoots(roots: List<File>): List<File> = withContext(Dispatchers.IO) {
        val result = mutableListOf<File>()
        val stack = ArrayDeque<File>()
        roots.forEach { if (it.exists()) stack.add(it) }
        while (stack.isNotEmpty()) {
            val f = stack.removeFirst()
            if (f.isDirectory) {
                f.listFiles()?.forEach { stack.add(it) }
            } else {
                result.add(f)
            }
        }
        result
    }

    fun gatherInstalledApkFiles(context: Context): List<File> {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val result = mutableListOf<File>()
        for (app in apps) {
            try {
                val src = app.sourceDir
                if (src != null) result.add(File(src))
            } catch (e: Exception) { }
        }
        return result
    }
}

