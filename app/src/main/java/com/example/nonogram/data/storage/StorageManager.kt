package com.example.nonogram.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.math.sqrt

class StorageManager(private val context: Context) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val rootDir = File(context.filesDir, "puzzles")

    init {
        if (!rootDir.exists()) rootDir.mkdirs()
    }

    private fun getPuzzleDir(id: String): File {
        return File(rootDir, id).apply { if (!exists()) mkdirs() }
    }

    // --- PUZZLE DATA (puzzle.json) ---

    fun savePuzzleData(id: String, data: PuzzleData) {
        val file = File(getPuzzleDir(id), "puzzle.json")
        val text = json.encodeToString(data)
        file.writeText(text)
    }

    fun loadPuzzleData(id: String): PuzzleData? {
        val dir = getPuzzleDir(id)
        var file = File(dir, "puzzle.json")
        if (!file.exists()) {
            file = File(dir, "blocks.json")
        }
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

    // --- REALTIME PROGRESS (progress.json) ---

    fun saveProgress(id: String, progress: PuzzleProgress) {
        val file = File(getPuzzleDir(id), "progress.json")
        val text = json.encodeToString(progress)
        file.writeText(text)
    }

    fun loadProgress(id: String): PuzzleProgress? {
        val file = File(getPuzzleDir(id), "progress.json")
        if (!file.exists()) return null

        return try {
            json.decodeFromString(file.readText())
        } catch (e: Exception) {
            null
        }
    }

    fun renamePuzzle(id: String, newTitle: String) {
        val progress = loadProgress(id) ?: PuzzleProgress(puzzleId = id)
        val updated = progress.copy(title = newTitle)
        saveProgress(id, updated)
    }

    // --- META SCANNING ---

    fun loadMeta(): List<PuzzleItem> {
        val dirs = rootDir.listFiles()?.filter { it.isDirectory } ?: return emptyList()
        val result = mutableListOf<PuzzleItem>()

        for (dir in dirs) {
            val id = dir.name
            val puzzleData = loadPuzzleData(id) ?: continue
            val progress = loadProgress(id)

            val currentBlock = progress?.currentBlockIndex ?: 0
            val totalBlocks = puzzleData.blocks.size
            val isCompleted = currentBlock >= totalBlocks

            val matrixSizeText = if (puzzleData.blocks.isNotEmpty()) {
                val h = puzzleData.blocks[0].matrix.size
                val w = if (h > 0) puzzleData.blocks[0].matrix[0].size else 0
                "${w}×${h}"
            } else ""

            val title = progress?.title ?: id

            result.add(
                PuzzleItem(
                    id = id,
                    title = title,
                    matrixSizeText = matrixSizeText,
                    currentBlock = currentBlock,
                    totalBlocks = totalBlocks,
                    isCompleted = isCompleted
                )
            )
        }

        return result.sortedByDescending { it.id }
    }

    fun getResultImage(id: String): File {
        return File(getPuzzleDir(id), "result.png")
    }

    // --- PROGRESSIVE PNG GENERATION ---

    fun generateAndSaveProgressImage(id: String, puzzleData: PuzzleData, currentBlockIndex: Int) {
        if (puzzleData.blocks.isEmpty()) return

        val totalBlocks = puzzleData.blocks.size
        val blocksPerSide = sqrt(totalBlocks.toDouble()).toInt().coerceAtLeast(1)

        val blockHeight = puzzleData.blocks[0].matrix.size
        val blockWidth = if (blockHeight > 0) puzzleData.blocks[0].matrix[0].size else 0
        if (blockWidth == 0 || blockHeight == 0) return

        val imageWidth = blocksPerSide * blockWidth
        val imageHeight = ((totalBlocks + blocksPerSide - 1) / blocksPerSide) * blockHeight

        val bitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)

        for (bIndex in 0 until totalBlocks) {
            val block = puzzleData.blocks[bIndex]
            val by = bIndex / blocksPerSide
            val bx = bIndex % blocksPerSide

            val isBlockSolved = bIndex < currentBlockIndex || currentBlockIndex >= totalBlocks

            for (y in 0 until blockHeight) {
                for (x in 0 until blockWidth) {
                    val globalX = bx * blockWidth + x
                    val globalY = by * blockHeight + y

                    val color = if (isBlockSolved) {
                        val colorIdx = block.matrix[y][x]
                        block.usedColors.getOrElse(colorIdx) { AndroidColor.WHITE }
                    } else {
                        AndroidColor.LTGRAY
                    }

                    bitmap.setPixel(globalX, globalY, color)
                }
            }
        }

        val file = getResultImage(id)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    // --- EXPORT & IMPORT ---

    fun exportPuzzle(id: String, outputUri: Uri, context: Context) {
        val puzzleData = loadPuzzleData(id) ?: throw IllegalArgumentException("Puzzle $id not found")
        val jsonText = json.encodeToString(puzzleData)

        val resolver = context.contentResolver
        resolver.openOutputStream(outputUri)?.use { outputStream ->
            ZipOutputStream(BufferedOutputStream(outputStream)).use { zip ->
                zip.putNextEntry(ZipEntry("puzzle.json"))
                zip.write(jsonText.toByteArray())
                zip.closeEntry()
            }
        }
    }

    fun importPuzzle(uri: Uri, context: Context) {
        val resolver = context.contentResolver
        val newId = "puzzle_${System.currentTimeMillis()}"

        var puzzleJsonText: String? = null

        resolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(BufferedInputStream(inputStream)).use { zip ->
                var entry: ZipEntry?
                while (zip.nextEntry.also { entry = it } != null) {
                    val currentEntry = entry ?: continue
                    if (currentEntry.name == "puzzle.json" || currentEntry.name == "blocks.json") {
                        puzzleJsonText = zip.readBytes().decodeToString()
                    }
                    zip.closeEntry()
                }
            }
        }

        if (puzzleJsonText == null) {
            puzzleJsonText = resolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
        }

        val jsonContent = puzzleJsonText
        if (jsonContent != null) {
            val puzzleData: PuzzleData = json.decodeFromString(jsonContent)
            savePuzzleData(newId, puzzleData)
            saveProgress(newId, PuzzleProgress(puzzleId = newId))
            generateAndSaveProgressImage(newId, puzzleData, 0)
        }
    }
}
