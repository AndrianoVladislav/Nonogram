package com.example.nonogram.data.storage

data class PlayerCell(
    var color: Int? = null,
    val notes: MutableSet<Int> = mutableSetOf()
)
