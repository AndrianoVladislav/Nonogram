package com.example.nonogram.data.converter

import android.graphics.Bitmap
import com.example.nonogram.data.storage.QuantizationResult

object ImageConverterQuantization {

    fun quantizeKMeans(
        bitmap: Bitmap,
        k: Int = 6,
        iterations: Int = 15
    ): QuantizationResult {

        val width = bitmap.width
        val height = bitmap.height

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // 1. Инициализация центров
        val centers = pixels.toList().shuffled().take(k).toMutableList()

        val assignments = IntArray(pixels.size)

        repeat(iterations) {

            // 2. Назначение кластеров
            for (i in pixels.indices) {
                assignments[i] = findClosestCenter(pixels[i], centers)
            }

            // 3. Пересчёт центров
            val sumR = IntArray(k)
            val sumG = IntArray(k)
            val sumB = IntArray(k)
            val count = IntArray(k)

            for (i in pixels.indices) {
                val cluster = assignments[i]
                val color = pixels[i]

                sumR[cluster] += (color shr 16) and 0xff
                sumG[cluster] += (color shr 8) and 0xff
                sumB[cluster] += color and 0xff
                count[cluster]++
            }

            for (i in 0 until k) {
                if (count[i] == 0) continue

                val r = sumR[i] / count[i]
                val g = sumG[i] / count[i]
                val b = sumB[i] / count[i]

                centers[i] = (0xff shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        // 4. Формируем solution
        val solution = Array(height) { IntArray(width) }

        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                solution[y][x] = assignments[index]
            }
        }


        //!!!!!!!!!!!!!!!
//        val counts = IntArray(k)
//        assignments.forEach { counts[it]++ }
//
//        val sortedIndices = counts.indices.sortedByDescending { counts[it] }

        return QuantizationResult(centers, solution)
    }

    private fun findClosestCenter(color: Int, centers: List<Int>): Int {

        var minDist = Double.MAX_VALUE
        var bestIndex = 0

        val r = (color shr 16) and 0xff
        val g = (color shr 8) and 0xff
        val b = color and 0xff

        for (i in centers.indices) {

            val cr = (centers[i] shr 16) and 0xff
            val cg = (centers[i] shr 8) and 0xff
            val cb = centers[i] and 0xff

            val dist = colorDistance(r, g, b, cr, cg, cb)

            if (dist < minDist) {
                minDist = dist
                bestIndex = i
            }
        }

        return bestIndex
    }

    private fun colorDistance(
        r1: Int, g1: Int, b1: Int,
        r2: Int, g2: Int, b2: Int
    ): Double {

        val dr = (r1 - r2)
        val dg = (g1 - g2)
        val db = (b1 - b2)

        return (dr * dr + dg * dg + db * db).toDouble()
    }
}