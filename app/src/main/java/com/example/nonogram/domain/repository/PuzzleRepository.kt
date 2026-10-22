package com.example.nonogram.domain.repository

import android.net.Uri
import com.example.nonogram.data.storage.Block
import com.example.nonogram.data.storage.PuzzleItem
import java.io.File

interface PuzzleRepository {
    suspend fun getPuzzles(): List<PuzzleItem>
    suspend fun getPuzzle(id: String): PuzzleItem?
    suspend fun getBlock(id: String, index: Int): Block?
    suspend fun updateProgress(id: String, newBlock: Int)
    suspend fun getResultImageFile(id: String): File
    suspend fun importPuzzle(uri: Uri): Result<Unit>
    suspend fun exportPuzzle(id: String, outputUri: Uri): Result<Unit>
    suspend fun createPuzzleFromImage(uri: Uri): Result<Unit>
}
