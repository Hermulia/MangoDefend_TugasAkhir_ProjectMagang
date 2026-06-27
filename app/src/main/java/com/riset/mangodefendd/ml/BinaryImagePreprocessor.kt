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
        val len = file.length()
        val step = maxOf(1.0, len.toDouble() / totalPixels)
        
        file.inputStream().use { input ->
            for (i in 0 until totalPixels) {
                val targetByteIndex = (i * step).toLong().coerceAtMost(maxOf(0L, len - 1))
                // This is a naive sampling: we skip to the target byte.
                // In a real app, you might want to read chunks or use a more efficient way to sample.
                // But for 224x224 = 50,176 pixels, this is manageable if we don't reload the file.
                
                // For efficiency, we should seek if possible, but InputStream doesn't support seek.
                // However, we are moving forward, so we can skip.
                // Wait, (i * step) is always increasing. So we can skip the difference.
                val currentPos = (if (i == 0) 0 else ((i - 1) * step).toLong()).coerceAtMost(maxOf(0L, len - 1))
                val toSkip = targetByteIndex - currentPos
                if (toSkip > 0) {
                    input.skip(toSkip)
                }
                
                val b = input.read().let { if (it == -1) 0 else it }
                val v = if (options.normalizeToZeroOne) b / 255f else b.toFloat()
                
                val y = i / size
                val x = i % size
                
                result[0][0][y][x] = v
                result[0][1][y][x] = v
                result[0][2][y][x] = v
            }
        }
        
        return result
    }
}

