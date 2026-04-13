package com.example.nonogram.data.storage

import kotlinx.serialization.Serializable

@Serializable
data class RLE(
    val color: Int,
    val count: Int
)