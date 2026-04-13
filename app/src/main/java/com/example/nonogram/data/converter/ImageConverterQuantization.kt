package com.example.nonogram.data.converter

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.example.nonogram.data.storage.QuantizationResult
import kotlin.random.Random

object ImageConverterQuantization {

    fun quantizeKMeans(
        bitmap: Bitmap,
        k: Int = 6,
        iterations: Int = 10
    ): QuantizationResult {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val pixelsLab = Array(pixels.size) { i ->
            DoubleArray(3).also { ColorUtils.colorToLAB(pixels[i], it) }
        }

        // Inicializace centrů
        val centers = initKMeansPP(pixelsLab, k)
        val assignments = IntArray(pixels.size)

        repeat(iterations) {
            // Přiřazení
            for (i in pixelsLab.indices) {
                val pixel = pixelsLab[i]
                var best = 0
                var minDist = Double.MAX_VALUE

                for (center in centers.indices) {
                    val dist = labDistance(pixel, centers[center])
                    if (dist < minDist) {
                        minDist = dist
                        best = center
                    }
                }
                assignments[i] = best
            }

            // Aktualizace
            val sums = Array(k) { DoubleArray(3) }
            val counts = IntArray(k)

            for (i in pixelsLab.indices) {
                val cluster = assignments[i]
                val pixel = pixelsLab[i]
                sums[cluster][0] += pixel[0]
                sums[cluster][1] += pixel[1]
                sums[cluster][2] += pixel[2]
                counts[cluster]++
            }

            // Řešení prázdných klastrů
            for (i in 0 until k) {
                if (counts[i] == 0) {
                    centers[i] = pixelsLab.random()
                    continue
                }
                centers[i][0] = sums[i][0] / counts[i]
                centers[i][1] = sums[i][1] / counts[i]
                centers[i][2] = sums[i][2] / counts[i]
            }
        }

        val palette = centers.map { Color(ColorUtils.LABToColor(it[0], it[1], it[2])) }

        val solution = Array(height) { y ->
            IntArray(width) { x -> assignments[y * width + x] }
        }

        return QuantizationResult(solution, palette)
    }

    private fun initKMeansPP(pixelsLab: Array<DoubleArray>, k: Int): MutableList<DoubleArray> {
        if (pixelsLab.isEmpty()) return mutableListOf()

        val centers = mutableListOf<DoubleArray>()
        centers.add(pixelsLab.random().copyOf())

        val minDistances = DoubleArray(pixelsLab.size) { Double.MAX_VALUE }

        while (centers.size < k) {
            val lastCenter = centers.last()
            var totalWeight = 0.0

            for (i in pixelsLab.indices) {
                val d = labDistance(pixelsLab[i], lastCenter)
                if (d < minDistances[i]) {
                    minDistances[i] = d
                }
                totalWeight += minDistances[i]
            }

            val r = Random.nextDouble() * totalWeight
            var cumulative = 0.0
            for (i in pixelsLab.indices) {
                cumulative += minDistances[i]
                if (cumulative >= r) {
                    centers.add(pixelsLab[i].copyOf())
                    break
                }
            }
        }
        return centers
    }

    private fun labDistance(c1: DoubleArray, c2: DoubleArray): Double {
        val dL = c1[0] - c2[0]
        val dA = c1[1] - c2[1]
        val dB = c1[2] - c2[2]
        return dL * dL + dA * dA + dB * dB
    }

    fun quantizedToBitmap(
        quantization: QuantizationResult
    ): Bitmap {
        val height = quantization.solution.size
        val width = quantization.solution[0].size

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val colorIndex = quantization.solution[y][x]
                val color = quantization.palette[colorIndex].toArgb()
                bitmap.setPixel(x, y, color)
            }
        }

        return bitmap
    }
}