package com.example.nonogram.data.storage

import kotlinx.serialization.Serializable

@Serializable
data class Block(
    val matrix: List<List<Int>>,
    val usedColors: List<Int>
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
