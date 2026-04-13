package com.example.nonogram.data.storage

data class Hint(
    val length: Int,
    val color: Int
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Hint

        if (length != other.length) return false
        if (color != other.color) return false

        return true
    }

    override fun hashCode(): Int {
        var result = length
        result = 31 * result + color
        return result
    }
}
