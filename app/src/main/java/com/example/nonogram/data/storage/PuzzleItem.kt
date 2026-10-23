package com.example.nonogram.data.storage

import kotlinx.serialization.Serializable

@Serializable
data class PuzzleItem(
    val id: String,
    val currentBlock: Int = 0,
    val totalBlocks: Int = 1,
    val isCompleted: Boolean = false
)
