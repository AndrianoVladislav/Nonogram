package com.example.nonogram.domain.repository

import android.net.Uri
import com.example.nonogram.data.storage.Block
import com.example.nonogram.data.storage.PuzzleData
import com.example.nonogram.data.storage.PuzzleItem
import com.example.nonogram.data.storage.PuzzleProgress
import java.io.File

interface PuzzleRepository {
    suspend fun getPuzzles(): List<PuzzleItem>
    suspend fun getPuzzleItem(id: String): PuzzleItem?
    suspend fun getPuzzleData(id: String): PuzzleData?
    suspend fun getBlock(id: String, index: Int): Block?
    suspend fun saveProgress(id: String, progress: PuzzleProgress)
    suspend fun loadProgress(id: String): PuzzleProgress?
    suspend fun getResultImageFile(id: String): File
    suspend fun importPuzzle(uri: Uri): Result<Unit>
    suspend fun exportPuzzle(id: String, outputUri: Uri): Result<Unit>
    suspend fun createPuzzleFromImage(uri: Uri): Result<Unit>
    suspend fun updateBlockProgressAndGenerateImage(id: String, completedBlockIndex: Int)
}
