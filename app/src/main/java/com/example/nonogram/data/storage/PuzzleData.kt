package com.example.nonogram.data.storage

import kotlinx.serialization.Serializable

@Serializable
data class PuzzleData(
    val blocks: List<Block>,
    val palette: List<Int>
)
