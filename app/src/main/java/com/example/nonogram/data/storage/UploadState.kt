package com.example.nonogram.data.storage

data class UploadState(
    val loading: Boolean = false,
    val progress: Float = 0f,
    val finished: Boolean = false,
    val error: String? = null
)
