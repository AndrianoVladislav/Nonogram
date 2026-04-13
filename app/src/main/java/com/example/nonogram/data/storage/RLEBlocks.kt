package com.example.nonogram.data.storage

import kotlinx.serialization.Serializable

@Serializable
data class RLEBlocks(
    val blocks: List<List<List<RLE>>>,
    val palette: List<Int>
)
