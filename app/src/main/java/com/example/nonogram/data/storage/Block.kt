package com.example.nonogram.data.storage

import androidx.compose.ui.graphics.Color

data class Block(
    val matrix: ArrayList<IntArray>,
    val usedColors: List<Color>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Block

        if (matrix != other.matrix) return false
        if (usedColors != other.usedColors) return false

        return true
    }

    override fun hashCode(): Int {
        var result = matrix.hashCode()
        result = 31 * result + usedColors.hashCode()
        return result
    }
}
