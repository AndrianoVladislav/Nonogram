package com.example.nonogram.data.storage

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.set
import androidx.core.graphics.createBitmap

data class QuantizationResult(
    val solution: Array<IntArray>,
    val palette: List<Color>
) {

    fun quantizedToBitmap(): Bitmap {

        val height =solution.size
        val width = solution[0].size

        val bitmap = createBitmap(width, height)

        for (y in 0 until height) {
            for (x in 0 until width) {

                val colorIndex = solution[y][x]
                val color = palette[colorIndex]

                bitmap[x, y] = color.toArgb()
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
