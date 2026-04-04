package com.example.nonogram.data.storage

import android.graphics.Bitmap

data class QuantizationResult(
    val palette: List<Int>,          // цвета (ARGB)
    val solution: Array<IntArray>    // индексы цветов
) {

    fun quantizedToBitmap(): Bitmap {

        val height =solution.size
        val width = solution[0].size

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (y in 0 until height) {
            for (x in 0 until width) {

                val colorIndex = solution[y][x]
                val color = palette[colorIndex]

                bitmap.setPixel(x, y, color)
            }
        }

        return bitmap
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QuantizationResult

        if (palette != other.palette) return false
        if (!solution.contentDeepEquals(other.solution)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = palette.hashCode()
        result = 31 * result + solution.contentDeepHashCode()
        return result
    }
}
