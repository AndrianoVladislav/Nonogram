package com.example.nonogram.data.storage

import kotlinx.serialization.Serializable

@Serializable
data class PuzzleItem(
    val id: String,
    var currentBlock: Int,
    val totalBlocks: Int
)
