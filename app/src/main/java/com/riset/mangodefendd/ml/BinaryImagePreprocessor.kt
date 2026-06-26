package com.riset.mangodefendd.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.BitmapFactory
import androidx.annotation.WorkerThread
import java.io.File

data class PreprocessOptions(
    val targetSize: Int = 224,
    val normalizeToZeroOne: Boolean = true
)

class BinaryImagePreprocessor(private val context: Context, private val options: PreprocessOptions = PreprocessOptions()) {

    @WorkerThread
    fun preprocessFileToTensor(file: File): Array<Array<Array<FloatArray>>> {
        val size = options.targetSize
        val result = Array(1) { Array(3) { Array(size) { FloatArray(size) } } }

        val extension = file.extension.lowercase()
        val isImage = extension == "png" || extension == "jpg" || extension == "jpeg" || extension == "bmp" || extension == "webp"

        if (isImage) {
            // Case 1: File is an image (visualization result). Extract actual pixels.
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, size, size, true)
                for (y in 0 until size) {
                    for (x in 0 until size) {
                        val pixel = resizedBitmap.getPixel(x, y)
                        // Use grayscale value from the image
                        val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3f
                        val v = if (options.normalizeToZeroOne) gray / 255f else gray
                        
                        result[0][0][y][x] = v
                        result[0][1][y][x] = v
                        result[0][2][y][x] = v
                    }
                }
                bitmap.recycle()
                if (resizedBitmap != bitmap) resizedBitmap.recycle()
                return result
            }
        }

        // Case 2: File is a binary (APK, EXE, etc.). Use raw byte sampling.
        val totalPixels = size * size
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

