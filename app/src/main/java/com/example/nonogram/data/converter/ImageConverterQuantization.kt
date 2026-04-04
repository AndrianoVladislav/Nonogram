package com.example.nonogram.data.converter

import android.graphics.Bitmap
import com.example.nonogram.data.storage.QuantizationResult

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

        val pixelsLab = pixels.map { rgbToLab(it) }

        val centers = initKMeansPP(pixelsLab, k)

        val assignments = IntArray(pixels.size)

        repeat(iterations) {

            // assign
            for (i in pixels.indices) {
                val p = pixelsLab[i]

                var best = 0
                var minDist = Double.MAX_VALUE

                for (c in centers.indices) {
                    val dist = labDistance(p, centers[c])
                    if (dist < minDist) {
                        minDist = dist
                        best = c
                    }
                }

                assignments[i] = best
            }

            // recompute
            val sum = Array(k) { DoubleArray(3) }
            val count = IntArray(k)

            for (i in pixels.indices) {
                val cluster = assignments[i]
                val p = pixelsLab[i]

                sum[cluster][0] += p[0]
                sum[cluster][1] += p[1]
                sum[cluster][2] += p[2]
                count[cluster]++
            }

            for (i in 0 until k) {
                if (count[i] == 0) continue

                centers[i][0] = sum[i][0] / count[i]
                centers[i][1] = sum[i][1] / count[i]
                centers[i][2] = sum[i][2] / count[i]
            }
        }

        // LAB → RGB (palette)
        val palette = centers.map { labToRgb(it) }

        val solution = Array(height) { IntArray(width) }

        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = y * width + x
                solution[y][x] = assignments[i]
            }
        }

        return QuantizationResult(palette, solution)
    }

    private fun initKMeansPP(pixelsLab: List<DoubleArray>, k: Int): MutableList<DoubleArray> {

        val centers = mutableListOf<DoubleArray>()

        // 1. первый центр
        centers.add(pixelsLab.random())

        while (centers.size < k) {

            val distances = pixelsLab.map { p ->
                centers.minOf { c -> labDistance(p, c) }
            }

            val sum = distances.sum()
            val r = Math.random() * sum

            var cumulative = 0.0

            for (i in pixelsLab.indices) {
                cumulative += distances[i]
                if (cumulative >= r) {
                    centers.add(pixelsLab[i])
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

    private fun rgbToLab(color: Int): DoubleArray {

        val r = ((color shr 16) and 0xff) / 255.0
        val g = ((color shr 8) and 0xff) / 255.0
        val b = (color and 0xff) / 255.0

        // gamma correction
        fun pivot(c: Double) =
            if (c > 0.04045) Math.pow((c + 0.055) / 1.055, 2.4)
            else c / 12.92

        val R = pivot(r)
        val G = pivot(g)
        val B = pivot(b)

        // RGB → XYZ
        val X = R * 0.4124 + G * 0.3576 + B * 0.1805
        val Y = R * 0.2126 + G * 0.7152 + B * 0.0722
        val Z = R * 0.0193 + G * 0.1192 + B * 0.9505

        // нормализация (D65)
        val Xn = 0.95047
        val Yn = 1.0
        val Zn = 1.08883

        fun f(t: Double) =
            if (t > 0.008856) Math.cbrt(t)
            else (7.787 * t + 16.0 / 116.0)

        val fx = f(X / Xn)
        val fy = f(Y / Yn)
        val fz = f(Z / Zn)

        val L = 116 * fy - 16
        val A = 500 * (fx - fy)
        val Bc = 200 * (fy - fz)

        return doubleArrayOf(L, A, Bc)
    }

    private fun labToRgb(lab: DoubleArray): Int {

        val L = lab[0]
        val A = lab[1]
        val B = lab[2]

        val fy = (L + 16) / 116
        val fx = A / 500 + fy
        val fz = fy - B / 200

        fun invf(t: Double) =
            if (t * t * t > 0.008856) t * t * t
            else (t - 16.0 / 116.0) / 7.787

        val X = 0.95047 * invf(fx)
        val Y = 1.0 * invf(fy)
        val Z = 1.08883 * invf(fz)

        var R = X * 3.2406 + Y * -1.5372 + Z * -0.4986
        var G = X * -0.9689 + Y * 1.8758 + Z * 0.0415
        var Bl = X * 0.0557 + Y * -0.2040 + Z * 1.0570

        fun clamp(v: Double) =
            (Math.max(0.0, Math.min(1.0, v)) * 255).toInt()

        return (0xff shl 24) or
                (clamp(R) shl 16) or
                (clamp(G) shl 8) or
                clamp(Bl)
    }
}