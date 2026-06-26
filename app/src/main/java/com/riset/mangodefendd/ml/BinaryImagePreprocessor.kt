package com.riset.mangodefendd.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.WorkerThread
import java.io.File
import kotlin.math.ceil
import kotlin.math.sqrt

data class PreprocessOptions(
    val targetSize: Int = 224,
    val normalizeToZeroOne: Boolean = true
)

class BinaryImagePreprocessor(private val context: Context, private val options: PreprocessOptions = PreprocessOptions()) {

    @WorkerThread
    fun preprocessFileToTensor(file: File): Array<Array<Array<FloatArray>>> {
        val size = options.targetSize
        val result = Array(1) { Array(3) { Array(size) { FloatArray(size) } } }
        val totalPixels = size * size
        
        // Direct byte sampling to avoid OutOfMemoryError on large files
        val bytes = file.readBytes()
        val len = bytes.size
        val step = maxOf(1.0, len.toDouble() / totalPixels)
        
        for (i in 0 until totalPixels) {
            val byteIndex = (i * step).toInt().coerceAtMost(maxOf(0, len - 1))
            val b = if (len > 0) (bytes[byteIndex].toInt() and 0xFF) else 0
            val v = if (options.normalizeToZeroOne) b / 255f else b.toFloat()
            
            val y = i / size
            val x = i % size
            
            result[0][0][y][x] = v
            result[0][1][y][x] = v
            result[0][2][y][x] = v
        }
        
        return result
    }
}

