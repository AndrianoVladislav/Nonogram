package com.example.nonogram.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

class StorageManager(private val context: Context) {

    private val json = Json { prettyPrint = true }

    private val rootDir = File(context.filesDir, "puzzles")

    init {
        if (!rootDir.exists()) rootDir.mkdirs()
    }

    private val metaFile = File(rootDir, "meta.json")

    fun loadMeta(): List<PuzzleItem> {

        if (!metaFile.exists()) return emptyList()

        return try {
            val text = metaFile.readText()
            json.decodeFromString(text)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveMeta(puzzles: List<PuzzleItem>) {

        val text = json.encodeToString(puzzles)
        metaFile.writeText(text)
    }

    private fun getPuzzleDir(id: String): File {
        return File(rootDir, id).apply { mkdirs() }
    }

    fun savePuzzleData(id: String, data: PuzzleData) {

        val dir = getPuzzleDir(id)
        val file = File(dir, "blocks.json")

        val text = json.encodeToString(data)
        file.writeText(text)
    }

    fun loadPuzzleData(id: String): PuzzleData? {

        val file = File(getPuzzleDir(id), "blocks.json")
        if (!file.exists()) return null

        return try {
            json.decodeFromString(file.readText())
        } catch (e: Exception) {
            null
        }
    }

    fun loadBlock(id: String, index: Int): Block? {

        val data = loadPuzzleData(id) ?: return null

        return data.blocks.getOrNull(index)
    }

    fun updateProgress(id: String, newBlock: Int) {

        val puzzles = loadMeta().toMutableList()

        val index = puzzles.indexOfFirst { it.id == id }
        if (index == -1) return

        val puzzle = puzzles[index]

        puzzles[index] = puzzle.copy(currentBlock = newBlock)

        saveMeta(puzzles)
    }

    fun isCompleted(puzzle: PuzzleItem): Boolean {
        return puzzle.currentBlock >= puzzle.totalBlocks
    }

    fun getResultImage(id: String): File {
        return File(getPuzzleDir(id), "result.png")
    }
}