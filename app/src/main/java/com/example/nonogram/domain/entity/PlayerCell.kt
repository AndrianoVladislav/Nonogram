package com.example.nonogram.domain.entity

data class PlayerCell(
    val color: Int? = null,
    val notes: Set<Int> = emptySet()
)
