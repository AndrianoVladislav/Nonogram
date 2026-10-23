package com.example.nonogram.data.repository

import android.content.Context
import android.net.Uri
import com.example.nonogram.data.converter.ImageConverterBlock
import com.example.nonogram.data.converter.ImageConverterQuantization
import com.example.nonogram.data.converter.ImageConverterResize
import com.example.nonogram.data.storage.Block
import com.example.nonogram.data.storage.ImageLoader
import com.example.nonogram.data.storage.PuzzleData
import com.example.nonogram.data.storage.PuzzleItem
import com.example.nonogram.data.storage.PuzzleProgress
import com.example.nonogram.data.storage.StorageManager
import com.example.nonogram.domain.repository.PuzzleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PuzzleRepositoryImpl(
    private val context: Context,
    private val storageManager: StorageManager = StorageManager(context)
) : PuzzleRepository {

    override suspend fun getPuzzles(): List<PuzzleItem> = withContext(Dispatchers.IO) {
        storageManager.loadMeta()
    }

    override suspend fun getPuzzleItem(id: String): PuzzleItem? = withContext(Dispatchers.IO) {
        storageManager.loadMeta().firstOrNull { it.id == id }
    }

    override suspend fun getPuzzleData(id: String): PuzzleData? = withContext(Dispatchers.IO) {
        storageManager.loadPuzzleData(id)
    }

    override suspend fun getBlock(id: String, index: Int): Block? = withContext(Dispatchers.IO) {
        storageManager.loadBlock(id, index)
    }

    override suspend fun saveProgress(id: String, progress: PuzzleProgress): Unit = withContext(Dispatchers.IO) {
        storageManager.saveProgress(id, progress)
    }

    override suspend fun loadProgress(id: String): PuzzleProgress? = withContext(Dispatchers.IO) {
        storageManager.loadProgress(id)
    }

    override suspend fun getResultImageFile(id: String): File = withContext(Dispatchers.IO) {
        storageManager.getResultImage(id)
    }

    override suspend fun importPuzzle(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            storageManager.importPuzzle(uri, context)
        }
    }

    override suspend fun exportPuzzle(id: String, outputUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            storageManager.exportPuzzle(id, outputUri, context)
        }
    }

    override suspend fun updateBlockProgressAndGenerateImage(
        id: String,
        completedBlockIndex: Int
    ): Unit = withContext(Dispatchers.IO) {
        val data = storageManager.loadPuzzleData(id) ?: return@withContext
        storageManager.generateAndSaveProgressImage(id, data, completedBlockIndex)
    }

    override suspend fun createPuzzleFromImage(uri: Uri): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val bitmap = ImageLoader.loadBitmapFromUri(context, uri)
                ?: throw IllegalArgumentException("Failed to load image bitmap from Uri")

            val resized = ImageConverterResize.resizeToBlocks(bitmap)
            val quantized = ImageConverterQuantization.quantizeKMeans(resized)
            val blocks = ImageConverterBlock.splitToBlocks(quantized)

            if (blocks.isEmpty()) {
                throw IllegalStateException("No puzzle blocks created from image")
            }

            val puzzleData = PuzzleData(
                palette = blocks[0].usedColors,
                blocks = blocks
            )

            val id = "puzzle_${System.currentTimeMillis()}"

            withContext(Dispatchers.IO) {
                storageManager.savePuzzleData(id, puzzleData)
                storageManager.saveProgress(id, PuzzleProgress(puzzleId = id))
                storageManager.generateAndSaveProgressImage(id, puzzleData, 0)
            }
        }
    }
}
