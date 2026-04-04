package com.example.nonogram.data.storage

data class Block(
    val cells: Array<IntArray>,
    val usedColors: Set<Int>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Block

        if (!cells.contentDeepEquals(other.cells)) return false
        if (usedColors != other.usedColors) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cells.contentDeepHashCode()
        result = 31 * result + usedColors.hashCode()
        return result
    }
}
