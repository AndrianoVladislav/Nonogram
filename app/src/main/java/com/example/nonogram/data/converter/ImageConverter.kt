package com.example.nonogram.data.converter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import androidx.compose.animation.core.copy
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import kotlin.math.ceil

object ImageConverter {

    /**
     * Конвертирует картинку в список блоков 25x25
     */
    fun convertToMatrix(source: Bitmap, blockSize: Int = 25): Array<BooleanArray> {
        // Ujistěte se, že zdrojový bitmap je softwarový bitmap pro kreslení na softwarové plátno
        //    Toto je klíčová oprava pro "Software rendering doesn't support hardware bitmaps"
        val softwareSourceBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            source.config == Bitmap.Config.HARDWARE) {
            source.copy(Bitmap.Config.ARGB_8888, false) // false = neměnný, pokud to stačí
        } else {
            source
        }

        // 1) Делаем размеры кратными blockSize, дополняя белыми пикселями
        val newWidth = (ceil(softwareSourceBitmap.width.toDouble() / blockSize) * blockSize).toInt()

        val newHeight = (ceil(softwareSourceBitmap.height.toDouble() / blockSize) * blockSize).toInt()

        val paddedBitmap = createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(paddedBitmap)
        canvas.drawColor(Color.WHITE) // заливаем белым
        canvas.drawBitmap(softwareSourceBitmap, 0f, 0f, Paint())

        // 2) Считаем адаптивный порог
        val threshold = calculateAdaptiveThreshold(paddedBitmap)

        // 3) Конвертируем в матрицу
        val fullMatrix = Array(newHeight) { BooleanArray(newWidth) }
        for (y in 0 until newHeight) {
            for (x in 0 until newWidth) {
                val pixel = paddedBitmap[x, y]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                fullMatrix[y][x] = gray < threshold
            }
        }
        return fullMatrix
    }

    fun convertMatrixToBlocks(matrix: Array<BooleanArray>, blockSize: Int = 25): List<Array<BooleanArray>> {
        // Разбиваем на блоки 25x25
        val blocks = mutableListOf<Array<BooleanArray>>()
        for (by in 0 until matrix.size step blockSize) {
            for (bx in 0 until matrix[0].size step blockSize) {
                val block = Array(blockSize) { BooleanArray(blockSize) }
                for (y in 0 until blockSize) {
                    for (x in 0 until blockSize) {
                        block[y][x] = matrix[by + y][bx + x]
                    }
                }
                blocks.add(block)
            }
        }
        return blocks
    }

    /**
     * Считает среднюю яркость картинки для адаптивного порога
     */
    private fun calculateAdaptiveThreshold(bitmap: Bitmap): Int {
        var sum = 0L
        var count = 0L
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = bitmap[x, y]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                sum += gray
                count++
            }
        }
        return (sum / count).toInt()
    }
}
