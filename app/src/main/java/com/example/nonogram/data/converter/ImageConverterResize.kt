package com.example.nonogram.data.converter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import kotlin.math.ceil
import androidx.core.graphics.set

object ImageConverterResize {

    fun resizeToBlocks(
        source: Bitmap,
        maxBlocks: Int = 20,
        blockSize: Int = 25
    ): Bitmap {

        val (targetW, targetH) = calculateTargetSize(
            source.width,
            source.height,
            maxBlocks,
            blockSize
        )

        return resizeBilinear(
            source,
            targetW,
            targetH
        )
    }

    private fun calculateTargetSize(
        originalWidth: Int,
        originalHeight: Int,
        maxBlocks: Int,
        blockSize: Int
    ): Pair<Int, Int> {

        val aspectRatio = originalWidth.toFloat() / originalHeight

        var bestW = 1
        var bestH = 1

        for (h in 1..maxBlocks) {
            for (w in 1..maxBlocks) {

                if (w * h > maxBlocks) continue

                val ratio = w.toFloat() / h
                val diff = kotlin.math.abs(ratio - aspectRatio)

                val bestDiff = kotlin.math.abs(bestW.toFloat() / bestH - aspectRatio)

                if (diff < bestDiff) {
                    bestW = w
                    bestH = h
                }
            }
        }

        return Pair(
            bestW * blockSize,
            bestH * blockSize
        )
    }

    fun resizeBilinear(
        src: Bitmap,
        newWidth: Int,
        newHeight: Int
    ): Bitmap {

        val result = createBitmap(newWidth, newHeight)

        val srcWidth = src.width
        val srcHeight = src.height
        val pixels = IntArray(srcWidth * srcHeight)
        src.getPixels(pixels, 0, srcWidth, 0, 0, srcWidth, srcHeight)

        for (y in 0 until newHeight) {

            val gy = y * (srcHeight - 1).toFloat() / (newHeight - 1)
            val y0 = gy.toInt()
            val y1 = minOf(y0 + 1, srcHeight - 1)
            val dy = gy - y0

            for (x in 0 until newWidth) {

                val gx = x * (srcWidth - 1).toFloat() / (newWidth - 1)
                val x0 = gx.toInt()
                val x1 = minOf(x0 + 1, srcWidth - 1)
                val dx = gx - x0

                val c00 = pixels[y0 * srcWidth + x0]
                val c10 = pixels[y0 * srcWidth + x1]
                val c01 = pixels[y1 * srcWidth + x0]
                val c11 = pixels[y1 * srcWidth + x1]

                val color = interpolateColor(c00, c10, c01, c11, dx, dy)

                result[x, y] = color
            }
        }

        return result
    }

    private fun interpolateColor(
        c00: Int,
        c10: Int,
        c01: Int,
        c11: Int,
        dx: Float,
        dy: Float
    ): Int {

        val a = bilerp(
            (c00 shr 24) and 0xff,
            (c10 shr 24) and 0xff,
            (c01 shr 24) and 0xff,
            (c11 shr 24) and 0xff,
            dx, dy
        )

        val r = bilerp(
            (c00 shr 16) and 0xff,
            (c10 shr 16) and 0xff,
            (c01 shr 16) and 0xff,
            (c11 shr 16) and 0xff,
            dx, dy
        )

        val g = bilerp(
            (c00 shr 8) and 0xff,
            (c10 shr 8) and 0xff,
            (c01 shr 8) and 0xff,
            (c11 shr 8) and 0xff,
            dx, dy
        )

        val b = bilerp(
            c00 and 0xff,
            c10 and 0xff,
            c01 and 0xff,
            c11 and 0xff,
            dx, dy
        )

        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun bilerp(
        c00: Int,
        c10: Int,
        c01: Int,
        c11: Int,
        dx: Float,
        dy: Float
    ): Int {

        val top = c00 * (1 - dx) + c10 * dx
        val bottom = c01 * (1 - dx) + c11 * dx

        return ((top * (1 - dy) + bottom * dy)).toInt().coerceIn(0, 255)
    }
}

object ImageConverter_prev {

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
