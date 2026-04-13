package com.example.nonogram.data.storage

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
data class PuzzleData(
    val blocks: List<Block>,
    val palette: List<Int>
)
